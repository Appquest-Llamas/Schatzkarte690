package com.example.schatzkarte690;

import java.io.File;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.MBTilesFileArchive;
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.TilesOverlay;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener {

	private final static int ACTIVITY_CHOOSE_FILE = 1;
	private final static int ACTIVITY_ACTIVATE_GPS = 2;

	private LocationManager locationManager;
	private MapView map;
	private SharedPreferences settings;
	private Button addMarkButton;
	private MyItemizedOverlay itemizedOverlay;
	private GeoPoint lastKnownGeoPoint;
	private EditText editTextOverLayItem;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			try {
				longToast("Aktivier GPS!");
				startActivityForResult(new Intent(
						android.provider.Settings.ACTION_SETTINGS),
						ACTIVITY_ACTIVATE_GPS);
			} catch (Exception e) {
			}
		} else {
			settings = getSharedPreferences("strings", 0);
			String tileFilePath = settings.getString("TileFilePath", null);
			if (tileFilePath == null || tileFilePath.isEmpty()) {
				Intent chooseFile;
				Intent intent;
				chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
				chooseFile.setType("file/*");
				intent = Intent.createChooser(chooseFile, "Choose a file");
				startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
			} else {
				File offlineMapFile = new File(tileFilePath);
				initMap(offlineMapFile);
			}
		}

	}

	@Override
	protected void onPause() {
		super.onPause();
		locationManager.removeUpdates(this);
		locationManager = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, // 1min
				0, // 10m
				this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		MenuItem item= menu.add(R.string.LogText);
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
				// TODO Auto-generated method stub
				log(itemizedOverlay);
				return false;
			}
		});
		return super.onCreateOptionsMenu(menu);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case ACTIVITY_CHOOSE_FILE: {
			if (resultCode == RESULT_OK) {
				try {
					Uri uri = data.getData();
					SharedPreferences settings = getSharedPreferences(
							"strings", 0);
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("TileFilePath", uri.getPath());
					editor.commit();
					onCreate(null);
				} catch (Exception e) {
					AlertDialog dialog = createMessageDialog(stackTraceToString(e
							.getStackTrace()));
					dialog.show();
				}
			}
			break;
		}
		case ACTIVITY_ACTIVATE_GPS: {
			if (resultCode == RESULT_OK) {
				onCreate(null);
				break;
			}
		}
		}

	}

	@Override
	public void onLocationChanged(Location location) {
		IMapController controller = map.getController();
		GeoPoint current = new GeoPoint(location);
		lastKnownGeoPoint = current;
		controller.setZoom(18);
		controller.animateTo(current);
		controller.setCenter(current);

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	private void initMap(File offlineMap) {
		GeoPoint hsrGeo = new GeoPoint(47.22324788, 8.817290017);
		if (offlineMap.exists()
				&& offlineMap.getAbsolutePath().endsWith(".mbtiles")) {
			LocationProvider provider = locationManager
					.getProvider(LocationManager.GPS_PROVIDER);
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, // 1min
					0, // 10m
					this);

			map = (MapView) findViewById(R.id.mapview);
			map.setTileSource(TileSourceFactory.MAPQUESTOSM);
			map.setMultiTouchControls(true);
			map.setBuiltInZoomControls(true);
			IMapController controller = map.getController();
			controller.setZoom(18);
			controller.animateTo(hsrGeo);
			XYTileSource mapTileSource = new XYTileSource("mbtiles",
					ResourceProxy.string.offline_mode, 1, 20, 256, ".png",
					"http://example.org/");
			MapTileModuleProviderBase treasureMapModuleProvider = new MapTileFileArchiveProvider(
					new SimpleRegisterReceiver(this), mapTileSource,
					new IArchiveFile[] { MBTilesFileArchive
							.getDatabaseFileArchive(offlineMap) });

			MapTileProviderBase treasureMapProvider = new MapTileProviderArray(
					mapTileSource,
					null,
					new MapTileModuleProviderBase[] { treasureMapModuleProvider });

			TilesOverlay treasureMapTilesOverlay = new TilesOverlay(
					treasureMapProvider, getBaseContext());
			treasureMapTilesOverlay
					.setLoadingBackgroundColor(Color.TRANSPARENT);

			// Jetzt können wir den Overlay zu unserer Karte hinzufügen:
			map.getOverlays().add(treasureMapTilesOverlay);
		} else {
			settings = getSharedPreferences("strings", 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putString("TileFilePath", "");
			editor.commit();

			LocationProvider provider = locationManager
					.getProvider(LocationManager.GPS_PROVIDER);
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, // 1min
					0, // 10m
					this);

			map = (MapView) findViewById(R.id.mapview);
			map.setTileSource(TileSourceFactory.MAPQUESTOSM);
			map.setMultiTouchControls(true);
			map.setBuiltInZoomControls(true);
			IMapController controller = map.getController();
			controller.setZoom(18);
			controller.animateTo(hsrGeo);

		}
		lastKnownGeoPoint = hsrGeo;
		Drawable marker = getResources().getDrawable(
				android.R.drawable.star_big_on);
		int markerWidth = marker.getIntrinsicWidth();
		int markerHeight = marker.getIntrinsicHeight();
		marker.setBounds(0, markerHeight, markerWidth, 0);

		ResourceProxy resourceProxy = new DefaultResourceProxyImpl(
				getApplicationContext());

		itemizedOverlay = new MyItemizedOverlay(marker, resourceProxy);
		map.getOverlays().add(itemizedOverlay);
		addMarkButton = (Button) findViewById(R.id.button_addMark);
		addMarkButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				editTextOverLayItem = (EditText) findViewById(R.id.editText_overLayItem);
				String text = "lat:" + lastKnownGeoPoint.getLatitude()
						+ "\nalt:" + lastKnownGeoPoint.getAltitude();
				itemizedOverlay.addItem(lastKnownGeoPoint, text,
						editTextOverLayItem.getText().toString());
			}

		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		outState.putParcelable("Marks", itemizedOverlay);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		itemizedOverlay = savedInstanceState.getParcelable("Marks");
		map.getOverlays().add(itemizedOverlay);
		super.onRestoreInstanceState(savedInstanceState);
	}

	private AlertDialog createMessageDialog(String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(message);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				dialog.dismiss();
				finish();

			}
		});
		return builder.create();
	}

	private void longToast(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	}

	public String stackTraceToString(StackTraceElement[] stacktrace) {
		String res = new String();
		for (int i = 0; i < stacktrace.length; i++) {
			res = res + stacktrace[i].toString();
		}
		return res;
	}

	private void log( MyItemizedOverlay itemizedOverlayToLog) {
		Intent intent = new Intent("ch.appquest.intent.LOG");

		if (getPackageManager().queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
			Toast.makeText(this, "Logbook App not Installed", Toast.LENGTH_LONG)
					.show();
			return;
		}

		intent.putExtra("ch.appquest.taskname", "Schatzkarte");
		intent.putExtra("ch.appquest.logmessage", markersToString(itemizedOverlayToLog));

		startActivity(intent);
	}
	private String markersToString(MyItemizedOverlay io) {
		String res=new String();
		for (int i=0;i<io.size();i++) {
			GeoPoint actual=io.createItem(i).getPoint();
			if(i==0)
				res+="("+actual.getLatitudeE6()+"/"+actual.getLongitudeE6()+")";
			else
				res+=",("+actual.getLatitudeE6()+"/"+actual.getLongitudeE6()+")";
		}
		return res;
	}
}
