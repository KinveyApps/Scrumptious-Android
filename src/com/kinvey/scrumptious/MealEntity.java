/** 
 * Copyright (c) 2013, Kinvey, Inc. All rights reserved.
 *
 * This software contains valuable confidential and proprietary information of
 * KINVEY, INC and is subject to applicable licensing agreements.
 * Unauthorized reproduction, transmission or distribution of this file and its
 * contents is a violation of applicable laws.
 * 
 */
package com.kinvey.scrumptious;

import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Bitmap;

import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class MealEntity extends GenericJson {
	@Key
	private String _id;
	@Key
	private double[] _geoloc;
	@Key
	private String determiner;
	@Key
	private String imageURL;
	@Key
	private double latitude;
	@Key
	private double longitude;
	@Key
	private String place;
	@Key
	private String selectedMeal;
	@Key
	private String[] tags;
	
	private Bitmap image;
	
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public double[] get_geoloc() {
		return _geoloc;
	}
	public void set_geoloc(double lat, double lon) {
		this._geoloc = new double[]{lon, lat};
	}
	public String getDeterminer() {
		return determiner;
	}
	public void setDeterminer(String determiner) {
		this.determiner = determiner;
	}
	public String getImageURL() {
		return imageURL;
	}
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude2) {
		this.latitude = latitude2;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public String getPlace() {
		return place;
	}
	public void setPlace(String place) {
		this.place = place;
	}
	public String getSelectedMeal() {
		return selectedMeal;
	}
	public void setSelectedMeal(String selectedMeal) {
		this.selectedMeal = selectedMeal;
	}
	public String[] getTags() {
		return tags;
	}
	public void setTags(String[] list) {
		this.tags = list;
	}	
}
