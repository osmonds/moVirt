package org.ovirt.mobile.movirt.ui;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import org.androidannotations.annotations.App;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Receiver;
import org.ovirt.mobile.movirt.Broadcasts;
import org.ovirt.mobile.movirt.MoVirtApp;
import org.ovirt.mobile.movirt.R;
import org.ovirt.mobile.movirt.provider.ProviderFacade;
import org.ovirt.mobile.movirt.sync.EventsHandler;
import org.ovirt.mobile.movirt.ui.auth.AuthenticatorActivity_;
import org.ovirt.mobile.movirt.util.message.CreateDialogBroadcastReceiver;
import org.ovirt.mobile.movirt.util.message.CreateDialogBroadcastReceiverHelper;
import org.ovirt.mobile.movirt.util.message.MessageHelper;
import org.ovirt.mobile.movirt.util.preferences.SettingsKey;
import org.ovirt.mobile.movirt.util.preferences.SharedPreferencesHelper;

import static org.ovirt.mobile.movirt.util.preferences.SettingsKey.MAX_EVENTS;
import static org.ovirt.mobile.movirt.util.preferences.SettingsKey.MAX_VMS;
import static org.ovirt.mobile.movirt.util.preferences.SettingsKey.PERIODIC_SYNC;
import static org.ovirt.mobile.movirt.util.preferences.SettingsKey.PERIODIC_SYNC_INTERVAL;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@EActivity
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener, CreateDialogBroadcastReceiver {

    private static final int OBJECTS_SAVE_LEVEL_THRESHOLD = 5000;
    @App
    MoVirtApp app;
    @Bean
    EventsHandler eventsHandler;
    @Bean
    SharedPreferencesHelper sharedPreferencesHelper;
    @Bean
    ProviderFacade providerFacade;
    @Bean
    MessageHelper messageHelper;

    private Preference periodicSyncIntervalPref;
    private Preference maxEventsPref;
    private Preference maxVmsPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference aboutButton = (Preference) findPreference("about_button");
        aboutButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Dialog dialog = new Dialog(SettingsActivity.this);
                dialog.setContentView(R.layout.about_dialog);
                dialog.setTitle(getString(R.string.prefs_about_moVirt));
                dialog.show();
                return true;
            }
        });

        Preference button = (Preference) findPreference("connection_button");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Intent intent = new Intent(
                        getApplicationContext(),
                        AuthenticatorActivity_.class);
                startActivity(intent);
                return true;
            }
        });

        periodicSyncIntervalPref = findPreference(PERIODIC_SYNC_INTERVAL.getValue());
        periodicSyncIntervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String errorMessage = "Interval should be not less then 1 minute.";
                int newValueInt;
                try {
                    newValueInt = Integer.parseInt((String) newValue);
                    if (newValueInt < 1) {
                        messageHelper.showToast(errorMessage);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    messageHelper.showToast(e.getMessage());
                    return false;
                }
                return true;
            }
        });
        maxEventsPref = findPreference(MAX_EVENTS.getValue());
        maxEventsPref.setOnPreferenceChangeListener(new IntegerValidator());
        maxVmsPref = findPreference(MAX_VMS.getValue());
        maxVmsPref.setOnPreferenceChangeListener(new IntegerValidator());
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        periodicSyncIntervalPref.setEnabled(sharedPreferencesHelper.getBooleanPref(PERIODIC_SYNC));

        setSyncIntervalPrefSummary();
        setMaxVmsSummary();
        setMaxEventsSummary();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String prefKey) {
        SettingsKey key = SettingsKey.from(prefKey);
        switch (key) {
            case PERIODIC_SYNC:
                periodicSyncIntervalPref.setEnabled(sharedPreferencesHelper.getBooleanPref(key));
                sharedPreferencesHelper.updatePeriodicSync();
                break;
            case PERIODIC_SYNC_INTERVAL:
                sharedPreferencesHelper.updatePeriodicSync();
                setSyncIntervalPrefSummary();
                break;
            case MAX_EVENTS:
                setMaxEventsSummary();
                eventsHandler.setMaxEventsStored(sharedPreferencesHelper.getIntPref(key));
                break;
            case MAX_VMS:
                setMaxVmsSummary();
                break;
        }
    }

    private void setMaxEventsSummary() {
        int maxEvents = sharedPreferencesHelper.getMaxEvents();
        maxEventsPref.setSummary(getString(
                R.string.prefs_max_events_locally_summary, maxEvents));
    }

    private void setMaxVmsSummary() {
        int maxVms = sharedPreferencesHelper.getMaxVms();
        maxVmsPref.setSummary(getString(
                R.string.prefs_max_vms_polled_summary, maxVms));
    }

    private void setSyncIntervalPrefSummary() {
        int interval = sharedPreferencesHelper.getPeriodicSyncInterval();
        periodicSyncIntervalPref.setSummary(getString(
                R.string.prefs_periodic_sync_interval_summary, interval));
    }

    private class IntegerValidator implements Preference.OnPreferenceChangeListener {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            try {
                int value = Integer.parseInt((String) newValue);
                informAboutMaxValues(value);
            } catch (NumberFormatException e) {
                messageHelper.showToast(e.getMessage());
                return false;
            }
            return true;
        }
    }

    private void informAboutMaxValues(int objectsLimit) {
        if (objectsLimit > OBJECTS_SAVE_LEVEL_THRESHOLD) {
            messageHelper.showToast(getString(R.string.objects_save_level_threshold_message));
        }
    }

    @Receiver(actions = {Broadcasts.ERROR_MESSAGE},
            registerAt = Receiver.RegisterAt.OnResumeOnPause)
    public void showErrorDialog(
            @Receiver.Extra(Broadcasts.Extras.ERROR_REASON) String reason,
            @Receiver.Extra(Broadcasts.Extras.REPEATED_MINOR_ERROR) boolean repeatedMinorError) {
        CreateDialogBroadcastReceiverHelper.showErrorDialog(getFragmentManager(), reason, repeatedMinorError);
    }

    @Receiver(actions = {Broadcasts.REST_CA_FAILURE},
            registerAt = Receiver.RegisterAt.OnResumeOnPause)
    public void showCertificateDialog(
            @Receiver.Extra(Broadcasts.Extras.ERROR_REASON) String reason) {
        CreateDialogBroadcastReceiverHelper.showCertificateDialog(getFragmentManager(), reason, true);
    }
}
