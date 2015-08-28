package org.sensors2.osc.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.CompoundButton;

import org.sensors2.common.dispatch.DataDispatcher;
import org.sensors2.common.sensors.Parameters;
import org.sensors2.common.sensors.SensorActivity;
import org.sensors2.common.sensors.SensorCommunication;
import org.sensors2.osc.R;
import org.sensors2.osc.dispatch.Bundling;
import org.sensors2.osc.dispatch.OscConfiguration;
import org.sensors2.osc.dispatch.OscDispatcher;
import org.sensors2.osc.fragments.SensorGroupFragment;
import org.sensors2.osc.sensors.Settings;

import java.util.ArrayList;
import java.util.List;


public class StartUpActivity extends FragmentActivity implements SensorActivity, CompoundButton.OnCheckedChangeListener {

	private Settings settings;
	private SensorCommunication sensorFactory;
	private OscDispatcher dispatcher;
	private SensorManager sensorManager;
	private CompoundButton activeButton;
	private PowerManager.WakeLock wakeLock;

	public Settings getSettings() {
		return this.settings;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start_up);

		this.settings = this.loadSettings();
		this.dispatcher = new OscDispatcher();
		this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		this.sensorFactory = new SensorCommunication(this);
		this.wakeLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this.getLocalClassName());

		this.activeButton = (CompoundButton) this.findViewById(R.id.active);
		this.activeButton.setOnCheckedChangeListener(this);
		for (Parameters parameters : this.sensorFactory.getSensors()) {
			this.CreateSensorFragments((org.sensors2.osc.sensors.Parameters) parameters);
		}
	}

	public List<Parameters> GetSensors(SensorManager sensorManager) {
		List<Parameters> parameters = new ArrayList<Parameters>();
		for (Sensor sensor : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
			parameters.add(new org.sensors2.osc.sensors.Parameters(sensor, this.getApplicationContext()));
//            if (sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
//                parameters.add(new org.sensors2.osc.sensors.Parameters(600, 9, "OrientationVector", "/orientationVector_sf"));
//                parameters.add(new org.sensors2.osc.sensors.Parameters(601, 16, "RotationMatrix", "/rotationMatrix_sf"));
//            }
		}
		return parameters;
	}

	@Override
	public DataDispatcher getDispatcher() {
		return this.dispatcher;
	}

	@Override
	public SensorManager getSensorManager() {
		return this.sensorManager;
	}

	private Settings loadSettings() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Settings settings = new Settings(preferences);
		OscConfiguration oscConfiguration = OscConfiguration.getInstance();
		oscConfiguration.setHost(settings.getHost());
		oscConfiguration.setPort(settings.getPort());
		return settings;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.start_up, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
			case R.id.action_settings: {
				Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				return true;
			}
			case R.id.action_guide: {
				Intent intent = new Intent(this, GuideActivity.class);
				startActivity(intent);
				return true;
			}
			case R.id.action_about: {
				Intent intent = new Intent(this, AboutActivity.class);
				startActivity(intent);
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		super.onPause();
		this.sensorFactory.onPause();
		if (this.wakeLock.isHeld()) {
			this.wakeLock.release();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.loadSettings();
		this.sensorFactory.onResume();
		if (this.activeButton.isChecked() && !this.wakeLock.isHeld()) {
			this.wakeLock.acquire();
		}
	}

	public void CreateSensorFragments(org.sensors2.osc.sensors.Parameters parameters) {
		FragmentManager manager = getSupportFragmentManager();
		SensorGroupFragment groupFragment = (SensorGroupFragment) manager.findFragmentByTag(parameters.getName());
		if (groupFragment == null) {
			this.CreateFragment(parameters, manager);
		}
	}

	public void CreateFragment(org.sensors2.osc.sensors.Parameters parameters, FragmentManager manager) {
		FragmentTransaction transaction = manager.beginTransaction();
		SensorGroupFragment groupFragment = new SensorGroupFragment();
		Bundle args = new Bundle();
		args.putInt(Bundling.DIMENSIONS, parameters.getDimensions());
		args.putInt(Bundling.SENSOR_TYPE, parameters.getSensorType());
		args.putString(Bundling.OSC_PREFIX, parameters.getOscPrefix());
		args.putString(Bundling.NAME, parameters.getName());
		groupFragment.setArguments(args);
		transaction.add(R.id.sensor_group, groupFragment, parameters.getName());
		transaction.commit();
	}

	public void addSensorGroupFragment(SensorGroupFragment sensorGroupFragment) {
		this.dispatcher.addSensorConfiguration(sensorGroupFragment.getSensorConfiguration());
	}

	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		if (activeButton.isChecked()) {
			this.sensorFactory.dispatch(sensorEvent.sensor.getType(), sensorEvent.timestamp, sensorEvent.values);
//            if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
//                float[] rotationV = new float[16];
//                SensorManager.getRotationMatrixFromVector(rotationV, sensorEvent.values);
//                float[] orientationValuesV = new float[9];
//                SensorManager.getOrientation(rotationV, orientationValuesV);
//                this.sensorFactory.dispatch(600, sensorEvent.timestamp, rotationV);
//                this.sensorFactory.dispatch(601, sensorEvent.timestamp, orientationValuesV);
//            }
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// We do not care about that
	}

	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
		if (isChecked) {
			if (!this.wakeLock.isHeld()) {
				this.wakeLock.acquire();
			}
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			this.wakeLock.release();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

	}
}
