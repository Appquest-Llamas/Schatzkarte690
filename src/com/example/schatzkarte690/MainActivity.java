package com.example.schatzkarte690;

import java.io.File;
import java.util.ArrayList;

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
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements LocationListener,
		RemoveItemCallback {

	private final static int ACTIVITY_CHOOSE_FILE = 1;
	private final static int ACTIVITY_ACTIVATE_GPS = 2;
	
	private final static int MILLISECONDSREQUEST=6000;
	private final static int METERSREQUEST=1;

	private LocationManager locationManager;
	private MapView map;
	private SharedPreferences settings;
	private Button addMarkButton;
	private MyItemizedOverlay itemizedOverlay;
	private GeoPoint lastKnownGeoPoint=null;
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
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MILLISECONDSREQUEST, // 1min
				METERSREQUEST,
				this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem item = menu.add(R.string.LogText);
		item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

			@Override
			public boolean onMenuItemClick(MenuItem arg0) {
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
			else if (resultCode==RESULT_CANCELED) {
				longToast("Select Offline Data!!");
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
		((TextView) findViewById(R.id.textView_latitude)).setText(Double
				.toString(location.getLatitude()));
		((TextView) findViewById(R.id.textView_longtitude)).setText(Double
				.toString(location.getLongitude()));
		lastKnownGeoPoint = current;
		controller.setCenter(current);

	}

	private void initMap(File offlineMap) {
		if (offlineMap.exists()
				&& offlineMap.getAbsolutePath().endsWith(".mbtiles")) {
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, MILLISECONDSREQUEST,
					METERSREQUEST,
					this);

			map = (MapView) findViewById(R.id.mapview);
			map.setTileSource(TileSourceFactory.MAPQUESTOSM);
			map.setMultiTouchControls(true);
			map.setBuiltInZoomControls(true);
			IMapController controller = map.getController();
			controller.setZoom(18);
			if(lastKnownGeoPoint==null)
				lastKnownGeoPoint=new GeoPoint(47.22324788, 8.817290017);
			controller.animateTo(lastKnownGeoPoint);
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
			
			map.getOverlays().add(treasureMapTilesOverlay);
		} else {
			longToast("Offline Data moved -> restart App");
			settings = getSharedPreferences("strings", 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.remove("TileFilePath");
			editor.commit();
			locationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, MILLISECONDSREQUEST, // 1min
					METERSREQUEST,
					this);

			map = (MapView) findViewById(R.id.mapview);
			map.setTileSource(TileSourceFactory.MAPQUESTOSM);
			map.setMultiTouchControls(true);
			map.setBuiltInZoomControls(true);
			IMapController controller = map.getController();
			controller.setZoom(18);
			if(lastKnownGeoPoint==null)
				lastKnownGeoPoint=new GeoPoint(47.22324788, 8.817290017);
			controller.animateTo(lastKnownGeoPoint);
		}
		Drawable marker = getResources().getDrawable(
				android.R.drawable.star_big_on);
		int markerWidth = marker.getIntrinsicWidth();
		int markerHeight = marker.getIntrinsicHeight();
		marker.setBounds(0, markerHeight, markerWidth, 0);

		ResourceProxy resourceProxy = new DefaultResourceProxyImpl(
				getApplicationContext());

		itemizedOverlay = new MyItemizedOverlay(marker, resourceProxy, this);
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
				map.invalidate();
			}

		});
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if(lastKnownGeoPoint!=null)
			outState.putString("lastKnownLocation",lastKnownGeoPoint.toString() );
		if(itemizedOverlay!=null)
			outState.putParcelable("Marks", itemizedOverlay);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		itemizedOverlay = savedInstanceState.getParcelable("Marks");
		lastKnownGeoPoint=getGeoPointForString(savedInstanceState.getString("lastKnownLocation"));
		IMapController controller=map.getController();
		controller.animateTo(lastKnownGeoPoint);
		map.getOverlays().add(itemizedOverlay);
		super.onRestoreInstanceState(savedInstanceState);
	}

	private void log(MyItemizedOverlay itemizedOverlayToLog) {
		Intent intent = new Intent("ch.appquest.intent.LOG");

		if (getPackageManager().queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY).isEmpty()) {
			Toast.makeText(this, "Logbook App not Installed", Toast.LENGTH_LONG)
					.show();
			return;
		}

		intent.putExtra("ch.appquest.taskname", "Schatzkarte");
		intent.putExtra("ch.appquest.logmessage",
				markersToString(itemizedOverlayToLog));

		startActivity(intent);
	}
	
	@Override
	public void removed(int index) {
		try {
			ArrayList<OverlayItem> overlayItems = ((MyItemizedOverlay) map.getOverlays()
					.get(1)).getOverlayItemList();
			
			MyItemizedOverlay newOverlay = new MyItemizedOverlay(itemizedOverlay.getMarker(),
					itemizedOverlay.getResourceProxy(), this);
			
			map.getOverlays().remove(itemizedOverlay);
			map.invalidate();
			if(overlayItems.size()>index)
				overlayItems.remove(index);
			for (int i = 0; i < overlayItems.size(); i++) {
				OverlayItem acualItem = overlayItems.get(i);
				newOverlay.addItem(acualItem.getPoint(), acualItem.getTitle(),
						acualItem.getSnippet());
			}
			itemizedOverlay = newOverlay;
			map.getOverlays().add(itemizedOverlay);
			map.invalidate();
		} catch (Exception e) {
			AlertDialog dialog=createMessageDialog(stackTraceToString(e.getStackTrace()));
			dialog.show();
		}
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
	private String markersToString(MyItemizedOverlay io) {
		String res = new String();
		for (int i = 0; i < io.size(); i++) {
			GeoPoint actual = io.createItem(i).getPoint();
			if (i == 0)
				res += "(" + actual.getLatitudeE6() + "/"
						+ actual.getLongitudeE6() + ")";
			else
				res += ",(" + actual.getLatitudeE6() + "/"
						+ actual.getLongitudeE6() + ")";
		}
		return res;
	}
	private GeoPoint getGeoPointForString(String geoString){
		String[] valuesStrings=geoString.split("[,]");
		return new GeoPoint(Integer.parseInt(valuesStrings[0]), Integer.parseInt(valuesStrings[1]));
	}
	@Override
	public void onProviderDisabled(String provider) {
	}
	@Override
	public void onProviderEnabled(String provider) {
	}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
