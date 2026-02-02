package OnboardingKn;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import net.sf.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Extension
public class OnboardingGlobalConfiguration extends GlobalConfiguration {

    private String name;
    private String description;

    private List<Connection> connections = new ArrayList<>();
    private List<Category> categories = new ArrayList<>();

    public OnboardingGlobalConfiguration() {
        load();

        if (categories.isEmpty()) {
            categories.add(new Category(UUID.randomUUID().toString(), "Development"));
            categories.add(new Category(UUID.randomUUID().toString(), "QA"));
            categories.add(new Category(UUID.randomUUID().toString(), "Production"));
            save();
        }
    }

    public static OnboardingGlobalConfiguration get() {
        return GlobalConfiguration.all().get(OnboardingGlobalConfiguration.class);
    }

    /* ---------- getters ---------- */
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Connection> getConnections() { return connections; }
    public List<Category> getCategories() { return categories; }

    /* ---------- UI validation ---------- */
    public FormValidation doCheckName(@QueryParameter String value) {
        if (value == null || value.isBlank()) return FormValidation.warning("Name should not be empty");
        if (!value.matches("[a-zA-Z ]+")) return FormValidation.warning("Only letters and spaces are allowed");
        return FormValidation.ok();
    }

    /* ---------- Save configuration ---------- */
    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) {
        req.bindJSON(this, json);
        save();
        return true;
    }

    /* ---------- Test connection ---------- */
    public FormValidation doTestConnection(
            @QueryParameter String username,
            @QueryParameter String password) {

        String url = this.name;

        if (url == null || url.isBlank()) {
            return FormValidation.error("Name field is empty; cannot use as URL");
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            if (username != null && !username.isBlank()) {
                String auth = username + ":" + password;
                String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }

            int code = conn.getResponseCode();
            return code == 200
                    ? FormValidation.ok("Connection successful (HTTP " + code + ")")
                    : FormValidation.warning("HTTP " + code);

        } catch (Exception e) {
            return FormValidation.error(e.getMessage());
        }
    }

    /* ---------- Last builds helper ---------- */
    public List<String> getLastBuilds(String categoryUuid) {
        OnboardingBuildHistory history = OnboardingBuildHistory.get();
        if (history != null) {
            return history.getBuilds(categoryUuid);
        }
        return new ArrayList<>();
    }

    /* ---------- Connection ---------- */
    public static class Connection extends hudson.model.AbstractDescribableImpl<Connection> {
        private String url;
        private String username;
        private Secret password;

        @DataBoundSetter public void setUrl(String url) { this.url = url; }
        @DataBoundSetter public void setUsername(String username) { this.username = username; }
        @DataBoundSetter public void setPassword(String password) { this.password = Secret.fromString(password); }

        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getPassword() { return Secret.toString(password); }

        @Extension
        public static class DescriptorImpl extends Descriptor<Connection> {
            @Override public String getDisplayName() { return "Connection"; }
        }
    }

    /* ---------- Category ---------- */
    public static class Category extends hudson.model.AbstractDescribableImpl<Category> {
        private final String uuid;
        private String name;

        @DataBoundSetter public void setName(String name) { this.name = name; }

        public Category(String uuid, String name) {
            this.uuid = (uuid == null || uuid.isBlank()) ? UUID.randomUUID().toString() : uuid;
            this.name = name;
        }

        public String getUuid() { return uuid; }
        public String getName() { return name; }

        @Extension
        public static class DescriptorImpl extends Descriptor<Category> {
            @Override public String getDisplayName() { return "Category"; }
        }
    }
}
