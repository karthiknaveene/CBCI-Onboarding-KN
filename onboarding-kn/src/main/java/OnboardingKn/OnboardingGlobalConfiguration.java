package OnboardingKn;

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.model.Descriptor.FormException;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Extension
public class OnboardingGlobalConfiguration extends GlobalConfiguration {

    private String name;
    private String description;

    private List<Connection> connections = new ArrayList<>();

    public OnboardingGlobalConfiguration() {
        load();
    }

    /* ---------- getters ---------- */
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<Connection> getConnections() { return connections; }

    /* ---------- UI validation ---------- */
    public FormValidation doCheckName(@QueryParameter String value) {
        if (value == null || value.isBlank()) {
            return FormValidation.warning("Name should not be empty");
        }
        if (!value.matches("[a-zA-Z ]+")) {
            return FormValidation.warning("Only letters and spaces are allowed");
        }
        return FormValidation.ok();
    }

    /* ---------- Save configuration ---------- */
    @Override
    public boolean configure(StaplerRequest2 req, JSONObject json) throws FormException {

        this.name = json.optString("name");
        this.description = json.optString("description");

        List<Connection> conns = new ArrayList<>();
        if (json.has("connections")) {
            JSONArray arr = json.getJSONArray("connections");
            for (Object o : arr) {
                JSONObject c = (JSONObject) o;
                Connection conn = new Connection(
                        c.optString("url"),
                        c.optString("username"),
                        c.optString("password")
                );
                conns.add(conn);
            }
        }
        this.connections = conns;

        save();
        return true;
    }

    /* ---------- Test connection (AJAX) ---------- */
    public FormValidation doTestConnection(
            @QueryParameter String url,
            @QueryParameter String username,
            @QueryParameter String password) {

        if (url == null || url.isBlank()) {
            return FormValidation.warning("URL is required");
        }

        try {
            URL endpoint = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
            conn.setRequestMethod("GET");

            if (username != null && !username.isBlank()) {
                String auth = username + ":" + password;
                String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Authorization", "Basic " + encoded);
            }

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status == 200) {
                return FormValidation.ok("Connection successful (HTTP 200)");
            } else {
                return FormValidation.warning("Connection failed. Server returned HTTP " + status);
            }

        } catch (Exception e) {
            return FormValidation.error("Connection error: " + e.getMessage());
        }
    }

    /* ---------- Connection model ---------- */
    public static class Connection {

        private String url;
        private String username;
        private Secret password;

        @DataBoundConstructor
        public Connection(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = Secret.fromString(password);
        }

        public String getUrl() { return url; }
        public String getUsername() { return username; }
        public String getPassword() { return Secret.toString(password); }

        @DataBoundSetter
        public void setUrl(String url) { this.url = url; }
        @DataBoundSetter
        public void setUsername(String username) { this.username = username; }
        @DataBoundSetter
        public void setPassword(String password) { this.password = Secret.fromString(password); }
    }
}
