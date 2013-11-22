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

public class MyItemizedOverlay extends ItemizedOverlay<OverlayItem> implements Parcelable{

	private ArrayList<OverlayItem> overlayItemList = new ArrayList<OverlayItem>();
	private boolean isRemoved=false;
	@Override
	protected boolean onTap(int index) {
		boolean tappedAnOverlay = super.onTap(index);
		if(tappedAnOverlay){
			overlayItemList.remove(index);
			setRemoved(true);
		}
		else {
			
		}
		return true;
	}
	
	
	public MyItemizedOverlay(Drawable pDefaultMarker,
			ResourceProxy pResourceProxy) {
		super(pDefaultMarker, pResourceProxy);
		// TODO Auto-generated constructor stub
	}

	public void addItem(GeoPoint p, String title, String snippet) {
		OverlayItem newItem = new OverlayItem(title, snippet, p);
		overlayItemList.add(newItem);
		populate();
	}

	@Override
	protected OverlayItem createItem(int arg0) {
		// TODO Auto-generated method stub
		return overlayItemList.get(arg0);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return overlayItemList.size();
	}

	
	@Override
	public boolean onSnapToItem(int arg0, int arg1, Point arg2, IMapView arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// TODO Auto-generated method stub
		
	}


	public boolean isRemoved() {
		return isRemoved;
	}


	public void setRemoved(boolean isRemoved) {
		this.isRemoved = isRemoved;
	}

}
