package com.netangel.netangelprotection;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.netangel.netangelprotection.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, constants = BuildConfig.class)
public abstract class NetAngelRobolectricSuite {

    @Before
    public final void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public final void teardown() {
        // good place to close any data connections
    }

    /**
     * Helper for starting a fragment inside a FragmentActivity.
     *
     * @param fragment the fragment to play.
     */
    public static <T extends Fragment> T startFragment(T fragment) {
        return startFragment(fragment, FragmentActivity.class);
    }

    public static <T extends android.app.Fragment> T startFragment(T fragment) {
        return startFragment(fragment, FragmentActivity.class);
    }

    public static <T extends Fragment> T startFragment(T fragment, Class<? extends FragmentActivity> activityClass) {
        FragmentActivity activity = Robolectric.buildActivity(activityClass)
                .create()
                .start()
                .get();

        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(fragment, null);
        fragmentTransaction.commit();

        return fragment;
    }

    public static <T extends android.app.Fragment> T startFragment(T fragment, Class<? extends Activity> activityClass) {
        Activity activity = Robolectric.buildActivity(activityClass)
                .create()
                .start()
                .get();

        android.app.FragmentManager fragmentManager = activity.getFragmentManager();
        android.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(fragment, null);
        fragmentTransaction.commit();

        return fragment;
    }

}

