package org.odk.collect.android.formentry;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.odk.collect.android.R;
import org.odk.collect.android.formentry.backgroundlocation.BackgroundLocationViewModel;
import org.odk.collect.android.formentry.questions.AnswersProvider;
import org.odk.collect.android.formentry.saving.FormSaveViewModel;
import org.odk.collect.android.javarosawrapper.FormController;
import org.odk.collect.audiorecorder.recording.AudioRecorderViewModel;
import org.robolectric.Robolectric;
import org.robolectric.annotation.LooperMode;
import org.robolectric.fakes.RoboMenu;
import org.robolectric.fakes.RoboMenuItem;

import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.support.RobolectricHelpers.createThemedActivity;
import static org.odk.collect.android.support.RobolectricHelpers.getFragmentByClass;
import static org.odk.collect.android.support.RobolectricHelpers.mockViewModelProvider;
import static org.robolectric.Shadows.shadowOf;

@RunWith(AndroidJUnit4.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class FormEntryMenuDelegateTest {

    private FormEntryMenuDelegate formEntryMenuDelegate;
    private AppCompatActivity activity;
    private FormEntryViewModel formEntryViewModel;
    private AnswersProvider answersProvider;
    private FormSaveViewModel formSaveViewModel;
    private AudioRecorderViewModel audioRecorderViewModel;

    @Before
    public void setup() {
        activity = createThemedActivity(AppCompatActivity.class, R.style.Theme_MaterialComponents);
        FormController formController = mock(FormController.class);
        answersProvider = mock(AnswersProvider.class);
        formEntryViewModel = mockViewModelProvider(activity, FormEntryViewModel.class).get(FormEntryViewModel.class);
        formSaveViewModel = mockViewModelProvider(activity, FormSaveViewModel.class).get(FormSaveViewModel.class);
        audioRecorderViewModel = mockViewModelProvider(activity, AudioRecorderViewModel.class).get(AudioRecorderViewModel.class);
        mockViewModelProvider(activity, BackgroundLocationViewModel.class).get(BackgroundLocationViewModel.class);

        formEntryMenuDelegate = new FormEntryMenuDelegate(activity, answersProvider, mock(FormIndexAnimationHandler.class));
        formEntryMenuDelegate.formLoaded(formController);
    }

    @Test
    public void onPrepare_inRepeatQuestion_showsAddRepeat() {
        when(formEntryViewModel.canAddRepeat()).thenReturn(true);

        RoboMenu menu = new RoboMenu();
        formEntryMenuDelegate.onCreateOptionsMenu(Robolectric.setupActivity(FragmentActivity.class).getMenuInflater(), menu);
        formEntryMenuDelegate.onPrepareOptionsMenu(menu);

        assertThat(menu.findItem(R.id.menu_add_repeat).isVisible(), equalTo(true));
    }

    @Test
    public void onPrepare_notInRepeatQuestion_hidesAddRepeat() {
        when(formEntryViewModel.canAddRepeat()).thenReturn(false);

        RoboMenu menu = new RoboMenu();
        formEntryMenuDelegate.onCreateOptionsMenu(Robolectric.setupActivity(FragmentActivity.class).getMenuInflater(), menu);
        formEntryMenuDelegate.onPrepareOptionsMenu(menu);

        assertThat(menu.findItem(R.id.menu_add_repeat).isVisible(), equalTo(false));
    }

    @Test
    public void onPrepare_whenFormControllerIsNull_hidesAddRepeat() {
        formEntryMenuDelegate = new FormEntryMenuDelegate(activity, answersProvider, mock(FormIndexAnimationHandler.class));
        formEntryMenuDelegate.formLoaded(null);

        RoboMenu menu = new RoboMenu();
        formEntryMenuDelegate.onCreateOptionsMenu(Robolectric.setupActivity(FragmentActivity.class).getMenuInflater(), menu);
        formEntryMenuDelegate.onPrepareOptionsMenu(menu);

        assertThat(menu.findItem(R.id.menu_add_repeat).isVisible(), equalTo(false));
    }

    @Test
    public void onItemSelected_whenAddRepeat_callsPromptForNewRepeat() {
        RoboMenu menu = new RoboMenu();
        formEntryMenuDelegate.onCreateOptionsMenu(Robolectric.setupActivity(FragmentActivity.class).getMenuInflater(), menu);
        formEntryMenuDelegate.onPrepareOptionsMenu(menu);

        formEntryMenuDelegate.onOptionsItemSelected(new RoboMenuItem(R.id.menu_add_repeat));
        verify(formEntryViewModel).promptForNewRepeat();
    }

    @Test
    public void onItemSelected_whenAddRepeat_savesScreenAnswers() {
        RoboMenu menu = new RoboMenu();
        formEntryMenuDelegate.onCreateOptionsMenu(Robolectric.setupActivity(FragmentActivity.class).getMenuInflater(), menu);
        formEntryMenuDelegate.onPrepareOptionsMenu(menu);

        HashMap answers = new HashMap();
        when(answersProvider.getAnswers()).thenReturn(answers);
        formEntryMenuDelegate.onOptionsItemSelected(new RoboMenuItem(R.id.menu_add_repeat));
        verify(formSaveViewModel).saveAnswersForScreen(answers);
    }

    @Test
    public void onItemSelected_whenPreferences_whenRecording_showsWarning() {
        RoboMenu menu = new RoboMenu();
        formEntryMenuDelegate.onCreateOptionsMenu(Robolectric.setupActivity(FragmentActivity.class).getMenuInflater(), menu);
        formEntryMenuDelegate.onPrepareOptionsMenu(menu);

        when(audioRecorderViewModel.isRecording()).thenReturn(new MutableLiveData<>(true));

        formEntryMenuDelegate.onOptionsItemSelected(new RoboMenuItem(R.id.menu_preferences));
        assertThat(shadowOf(activity).getNextStartedActivity(), is(nullValue()));

        Fragment dialog = getFragmentByClass(activity.getSupportFragmentManager(), RecordingWarningDialogFragment.class);
        assertThat(dialog, is(notNullValue()));
    }
}
