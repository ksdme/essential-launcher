package xyz.ksdme.essential.testing;

import java.io.IOException;
import android.Manifest;
import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.screenshot.Screenshot;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;

import de.clemensbartz.android.launcher.R;
import de.clemensbartz.android.launcher.Launcher;

public class ScreenShotTest {

    @Rule public ActivityTestRule<Launcher> mLaunchRule = new ActivityTestRule<Launcher>(
            Launcher.class, false, false);

    @Rule public GrantPermissionRule mPermissionsRule = GrantPermissionRule.grant(
            Manifest.permission.WRITE_EXTERNAL_STORAGE);

    private Launcher mLauncherActivity = null;

    private int ELEMENT_TO_TAP_TO_OPEN_APP_LIST = R.id.flWidget;
    private int ELEMENT_CONTAINING_APP_LIST_ITEMS = R.id.gvApplications;

    @Before
    public void launchActivity() {
        mLauncherActivity = mLaunchRule.launchActivity(null);
    }

    @Test
    public void testScreenshotPortraitApp() throws IOException {
        this.screenshot("portrait-home");

        Espresso.onView(withId(ELEMENT_TO_TAP_TO_OPEN_APP_LIST)).perform(click());
        Espresso.onView(withId(ELEMENT_CONTAINING_APP_LIST_ITEMS));
        this.screenshot("portrait-dock");
    }

    private void screenshot(String name) throws IOException {
        Screenshot.capture(mLauncherActivity).setName(name).process();
    }

}
