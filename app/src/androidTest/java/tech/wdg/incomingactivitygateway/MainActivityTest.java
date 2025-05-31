package tech.wdg.incomingactivitygateway;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        @Rule
        public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule(MainActivity.class);

        @Before
        public void clearSharedPrefs() {
                SharedPreferences sharedPreferences = context.getSharedPreferences(
                                context.getString(R.string.key_phones_preference),
                                Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.commit();
        }

        @After
        public void recreateActivity() {
                ActivityScenario<MainActivity> scenario = activityRule.getScenario();
                scenario.moveToState(Lifecycle.State.RESUMED);
                scenario.recreate();
        }

        @Test
        public void testAddDialogOpen() {
                onView(withId(R.id.btn_add)).perform(click());
                onView(withId(R.id.btn_add)).check(matches(isDisplayed()));
        }

        @Test
        public void testEmptySenderError() {
                onView(withId(R.id.btn_add)).perform(click());
        }

        @Test
        public void testEmptyUrlError() {
                onView(withId(R.id.btn_add)).perform(click());
        }

        @Test
        public void testWrongUrlError() {
                onView(withId(R.id.btn_add)).perform(click());
        }

        @Test
        public void testEmptyJsonTemplateError() {
                onView(withId(R.id.btn_add)).perform(click());
        }

        @Test
        public void testWrongJsonTemplateError() {
                onView(withId(R.id.btn_add)).perform(click());
        }

        @Test
        public void testEmptyJsonHeadersError() {
                onView(withId(R.id.btn_add)).perform(click());
        }

        @Test
        public void testWrongJsonHeadersError() {
                onView(withId(R.id.btn_add)).perform(click());
        }

        @Test
        public void testAddDeleteRecord() {
                onView(withId(R.id.recyclerView)).check(matches(isDisplayed()));
                onView(withId(R.id.btn_add)).check(matches(isDisplayed()));
        }

        private String getResourceString(int id) {
                Context targetContext = ApplicationProvider.getApplicationContext();
                return targetContext.getResources().getString(id);
        }
}
