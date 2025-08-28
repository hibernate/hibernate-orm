/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gavin King
 */
public abstract class Being {
	private long id;
	private String identity;
	private Location location;
	private List things = new ArrayList();
	private Map info = new HashMap();
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the identity.
	 */
	public String getIdentity() {
		return identity;
	}
	/**
	 * @param identity The identity to set.
	 */
	public void setIdentity(String identity) {
		this.identity = identity;
	}
	/**
	 * @return Returns the location.
	 */
	public Location getLocation() {
		return location;
	}
	/**
	 * @param location The location to set.
	 */
	public void setLocation(Location location) {
		this.location = location;
	}
	public String getSpecies() {
		return null;
	}

	public List getThings() {
		return things;
	}
	public void setThings(List things) {
		this.things = things;
	}
	public Map getInfo() {
		return info;
	}

	public void setInfo(Map info) {
		this.info = info;
	}

}
