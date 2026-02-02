package OnboardingKn;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest2;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Extension
public class OnboardingBuildHistory extends GlobalConfiguration {

    private static final int MAX_ENTRIES = 5;

    // Map<Category UUID -> list of last build identifiers)
    private Map<String, LinkedList<String>> history = new HashMap<>();

    private final File storageFile;

    public OnboardingBuildHistory() {
        storageFile = new File(Jenkins.get().getRootDir(), "onboarding-build-history.json");
        loadFile();
    }

    /** Load JSON file if it exists */
    private void loadFile() {
        if (storageFile.exists()) {
            try (FileReader reader = new FileReader(storageFile)) {
                JSONObject json = JSONObject.fromObject(net.sf.json.JSONSerializer.toJSON(reader));
                for (Object key : json.keySet()) {
                    String cat = key.toString();
                    LinkedList<String> builds = new LinkedList<>();
                    for (Object o : (List<?>) json.get(cat)) {
                        builds.add(o.toString());
                    }
                    history.put(cat, builds);
                }
            } catch (Exception e) {
                history.clear();
                e.printStackTrace();
            }
        }
    }

    /** Save history to JSON */
    private synchronized void saveFile() {
        try (FileWriter writer = new FileWriter(storageFile)) {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, LinkedList<String>> entry : history.entrySet()) {
                json.put(entry.getKey(), entry.getValue());
            }
            json.write(writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Singleton accessor */
    public static OnboardingBuildHistory get() {
        return GlobalConfiguration.all().get(OnboardingBuildHistory.class);
    }

    /** Add a build to a category */
    public synchronized void addBuild(String categoryUuid, Run<?, ?> run) {
        if (categoryUuid == null || run == null) return;

        String buildInfo = run.getParent().getFullName() + "#" + run.getNumber();

        history.computeIfAbsent(categoryUuid, k -> new LinkedList<>());
        LinkedList<String> builds = history.get(categoryUuid);

        builds.addFirst(buildInfo);
        while (builds.size() > MAX_ENTRIES) {
            builds.removeLast();
        }

        saveFile();
    }

    /** Get last builds for a category */
    public synchronized List<String> getBuilds(String categoryUuid) {
        return history.getOrDefault(categoryUuid, new LinkedList<>());
    }

    /** Update job name if renamed */
    public synchronized void updateJobName(String oldName, String newName) {
        for (LinkedList<String> builds : history.values()) {
            for (int i = 0; i < builds.size(); i++) {
                String b = builds.get(i);
                if (b.startsWith(oldName + "#")) {
                    String number = b.substring(oldName.length());
                    builds.set(i, newName + number);
                }
            }
        }
        saveFile();
    }

    /** Optional: get full map of all histories */
    public Map<String, List<String>> getAllHistory() {
        Map<String, List<String>> copy = new HashMap<>();
        history.forEach((k, v) -> copy.put(k, new ArrayList<>(v)));
        return copy;
    }

    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) {
        saveFile();
        return true;
    }
}
