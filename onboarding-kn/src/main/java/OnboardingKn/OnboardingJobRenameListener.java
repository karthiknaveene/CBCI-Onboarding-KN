package OnboardingKn;

import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

@Extension
public class OnboardingJobRenameListener extends ItemListener {

    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        OnboardingBuildHistory history = OnboardingBuildHistory.get();
        if (history != null) {
            history.updateJobName(oldName, newName);
        }
    }
}
