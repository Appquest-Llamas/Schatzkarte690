package com.example.schatzkarte690;

import java.util.ArrayList;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IMapView;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;

public class MyItemizedOverlay extends ItemizedOverlay<OverlayItem> implements
		Parcelable {

	private ArrayList<OverlayItem> overlayItemList = new ArrayList<OverlayItem>();
	private RemoveItemCallback callback;
	private Drawable marker;
	private ResourceProxy resourceProxy;
	private RemoveItemCallback removeItemCallback;

	public MyItemizedOverlay(Drawable pDefaultMarker,
			ResourceProxy pResourceProxy,RemoveItemCallback callback) {
		super(pDefaultMarker, pResourceProxy);
		this.callback=callback;
		this.setMarker(pDefaultMarker);
		this.setResourceProxy(pResourceProxy);
		this.setRemoveItemCallback(callback);
		// TODO Auto-generated constructor stub
	}
	@Override
	protected boolean onTap(int index) {
		super.onTap(index);
		callback.removed(index);
		return true;
	}

	public void addItem(GeoPoint p, String title, String snippet) {
		OverlayItem newItem = new OverlayItem(title, snippet, p);
		overlayItemList.add(newItem);
		populate();
	}
	
	public void remove(int index){
		overlayItemList.remove(index);
	}

	@Override
	protected OverlayItem createItem(int arg0) {
		return overlayItemList.get(arg0);
	}

	@Override
	public int size() {
		return overlayItemList.size();
	}

	@Override
	public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
		return false;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
	}
	public Drawable getMarker() {
		return marker;
	}
	public void setMarker(Drawable marker) {
		this.marker = marker;
	}
	public ResourceProxy getResourceProxy() {
		return resourceProxy;
	}
	public void setResourceProxy(ResourceProxy resourceProxy) {
		this.resourceProxy = resourceProxy;
	}
	public RemoveItemCallback getRemoveItemCallback() {
		return removeItemCallback;
	}
	public void setRemoveItemCallback(RemoveItemCallback removeItemCallback) {
		this.removeItemCallback = removeItemCallback;
	}

}
