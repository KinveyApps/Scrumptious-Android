package com.kinvey.scrumptious;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.facebook.model.GraphObject;
import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

public class OpenGraphEntity extends GenericJson {
	@Key
	private String entityId;
	@Key
	private String _id;
	@Key
	private String[] tags;
	public String getEntityId() {
		return entityId;
	}
	public void setEntityId(String entityId) {
		this.entityId = entityId;
	}
	public String get_id() {
		return _id;
	}
	public void set_id(String _id) {
		this._id = _id;
	}
	public String[] getTags() {
		return tags;
	}
	public void setTags(String[] tags) {
		this.tags = tags;
	}

	
}
