package de.jrx.ad.nodes;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.settings);
		
		//Hide the preference "eula"
		getPreferenceScreen().removePreference(this.findPreference("eula"));
	}
}