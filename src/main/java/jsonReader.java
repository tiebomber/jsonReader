import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Comparator;


public class jsonReader {

    private static Map<String, Integer> groupCounts = new HashMap<>();
    private static final String JSON_KEY = "uids";

    public static void main(String[] args) {

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Input archive full name: ");
            String archiveName = reader.readLine();
            List<String> jsonList = getJSONs(archiveName);
            mergeUsers(jsonList);
            System.out.println(maxGroupsUser(groupCounts));
        } catch (IOException| ArchiveException | CompressorException | NullPointerException | IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    public static void mergeUsers(List<String> jsonList) {
        for (String s : jsonList) {
            JSONObject object = new JSONObject(s);
            JSONArray array = object.getJSONArray(JSON_KEY);
            Integer count;
            String id;
            for (int j = 0; j < array.length(); j++) {
                id = array.get(j).toString();
                count = groupCounts.get(id);
                if (count == null)
                    groupCounts.put(id, 1);
                else
                    groupCounts.put(id, count + 1);
            }
        }
    }

    public static <K, V extends Comparable<V>> K maxGroupsUser(Map<K, V> map) {
        Optional<Map.Entry<K, V>> maxEntry = map.entrySet()
                .stream()
                .max(Comparator.comparing(Map.Entry::getValue)
                );
        return maxEntry.get().getKey();
    }

    public static List<String> getJSONs(String archiveName) throws IOException, CompressorException, ArchiveException, IllegalArgumentException, NullPointerException {

        if (archiveName == null)
            throw new NullPointerException("Archive name is null.");

        if (archiveName.isEmpty())
            throw new IllegalArgumentException("Archive name is empty.");

        try (InputStream fileInputStream = new BufferedInputStream(new FileInputStream(archiveName))) {
            if (isSupportedArchiveFormat(archiveName))
                return getJSONsFromArchive(fileInputStream);
            else
                return getJSONsFromCompressor(fileInputStream);
        }
    }

    public static boolean isSupportedArchiveFormat(String archiveName) {
        String archiveFormat = archiveName.substring(archiveName.lastIndexOf('.') + 1);
        switch (archiveFormat) {
            case "ar":
            case "arj":
            case "zip":
            case "tar":
            case "jar":
            case "cpio":
            case "dump":
            case "7z":
                return true;
            default:
                return false;
        }
    }

    private static List<String> getJSONsFromArchive(InputStream fileInputStream) throws IOException, ArchiveException {

        try(ArchiveInputStream archiveInputStream = new ArchiveStreamFactory().createArchiveInputStream(fileInputStream)) {
            return getJSONsList(archiveInputStream);
        }
    }

    private static List<String> getJSONsFromCompressor(InputStream fileInputStream) throws IOException, CompressorException {

        try(CompressorInputStream compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(fileInputStream);
            ArchiveInputStream archiveInputStream = new TarArchiveInputStream(compressorInputStream)){
            return getJSONsList(archiveInputStream);
        }

    }

    private static List<String> getJSONsList(ArchiveInputStream archiveInputStream) throws IOException {

        List<String> result = new ArrayList<>();
        ArchiveEntry entry;

        while ((entry = archiveInputStream.getNextEntry()) != null) {
            if (!archiveInputStream.canReadEntryData(entry)) {
                continue;
            }
            if (!entry.isDirectory() && isJSONFile(entry.getName())) {
                byte[] buf = new byte[(int) entry.getSize()];
                IOUtils.readFully(archiveInputStream, buf);
                result.add(new String(buf));
            }
        }
        return result;
    }

    private static boolean isJSONFile(String fileName) {
        return fileName.toLowerCase().matches(".+\\.json$");
    }

}
