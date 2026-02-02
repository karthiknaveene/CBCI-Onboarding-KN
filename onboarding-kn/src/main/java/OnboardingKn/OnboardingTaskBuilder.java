package OnboardingKn;

import hudson.Launcher;
import hudson.FilePath;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.ListBoxModel;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.Extension;

import java.io.IOException;
import java.util.List;

public class OnboardingTaskBuilder extends Builder implements SimpleBuildStep {

    private String categoryUuid;

    @DataBoundConstructor
    public OnboardingTaskBuilder() {}

    public String getCategoryUuid() { return categoryUuid; }

    @DataBoundSetter
    public void setCategoryUuid(String categoryUuid) { this.categoryUuid = categoryUuid; }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
            throws InterruptedException, IOException {

        OnboardingGlobalConfiguration cfg = OnboardingGlobalConfiguration.get();
        String categoryName = "UNKNOWN CATEGORY";

        if (cfg != null) {
            List<OnboardingGlobalConfiguration.Category> categories = cfg.getCategories();
            for (OnboardingGlobalConfiguration.Category c : categories) {
                if (c.getUuid().equals(categoryUuid)) {
                    categoryName = c.getName();
                    break;
                }
            }
        }

        listener.getLogger().println("[Onboarding] Selected category: " + categoryName);

        // Add build to history
        OnboardingBuildHistory history = OnboardingBuildHistory.get();
        if (history != null && categoryUuid != null && !categoryUuid.isBlank()) {
            history.addBuild(categoryUuid, run);
        }
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true; // freestyle jobs
        }

        @Override
        public String getDisplayName() {
            return "Onboarding Task";
        }

        public ListBoxModel doFillCategoryUuidItems() {
            ListBoxModel items = new ListBoxModel();
            OnboardingGlobalConfiguration cfg = OnboardingGlobalConfiguration.get();
            if (cfg != null) {
                for (OnboardingGlobalConfiguration.Category c : cfg.getCategories()) {
                    items.add(c.getName(), c.getUuid());
                }
            }
            return items;
        }
    }
}
