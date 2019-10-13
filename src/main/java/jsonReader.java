import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Input archive full name: ");
            String archiveName = reader.readLine();
            List<String> jsonList = getJSONListFromArchive(archiveName);
            mergeUsers(jsonList);
            System.out.println("User ID: " + maxGroupsUser(groupCounts));
        } catch (CompressorException | IOException e) {
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

    public static  <K, V extends Comparable<V>> K maxGroupsUser(Map<K, V> map) {
        Optional<Map.Entry<K, V>> maxEntry = map.entrySet()
                .stream()
                .max(Comparator.comparing(Map.Entry::getValue)
                );
        return maxEntry.get().getKey();
    }

    public static List<String> getJSONListFromArchive(String archiveName) throws IOException, CompressorException {

        if (archiveName == null || archiveName.isEmpty())
            throw new FileNotFoundException("Archive name is null or empty");

        List<String> result = new ArrayList<>();
        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(archiveName));
             CompressorInputStream compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(inputStream);
             ArchiveInputStream archiveInputStream = new TarArchiveInputStream(compressorInputStream)) {

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

        }
        return result;
    }

    private static boolean isJSONFile(String fileName) {
        return fileName.matches(".+\\.json$");
    }

}
