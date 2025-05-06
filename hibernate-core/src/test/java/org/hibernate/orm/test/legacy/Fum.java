/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Fum implements Serializable {
	private String fum;
	private FumCompositeID id;
	private Fum fo;
	private Qux[] quxArray;
	private Set friends;
	private Calendar lastUpdated;
	private String tString;
	private short vid;
	private short dupe;
	private MapComponent mapComponent = new MapComponent();

	public Fum() {}
	public Fum(FumCompositeID id) {
		this.id = id;
		friends = new HashSet();
		FumCompositeID fid = new FumCompositeID();
		fid.setShort( (short) ( id.short_ + 33 ) );
		fid.setString( id.string_ + "dd" );
		Fum f = new Fum();
		f.id = fid;
		f.fum="FRIEND";
		friends.add(f);
	}
	public String getFum() {
		return fum;
	}
	public void setFum(String fum) {
		this.fum = fum;
	}

	public FumCompositeID getId() {
		return id;
	}
	private void setId(FumCompositeID id) {
		this.id = id;
	}
	public Fum getFo() {
		return fo;
	}
	public void setFo(Fum fo) {
		this.fo = fo;
	}

	public Qux[] getQuxArray() {
		return quxArray;
	}
	public void setQuxArray(Qux[] quxArray) {
		this.quxArray = quxArray;
	}

	public Set getFriends() {
		return friends;
	}

	public void setFriends(Set friends) {
		this.friends = friends;
	}

	public Calendar getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Calendar calendar) {
		lastUpdated = calendar;
	}

	public String getTString() {
		return tString;
	}

	public void setTString(String string) {
		tString = string;
	}

	public short getDupe() {
		return dupe;
	}

	public void setDupe(short s) {
		dupe = s;
	}

	public static final class MapComponent implements Serializable {
		private Map fummap = new HashMap();
		private Map stringmap = new HashMap();
		private int count;
		public Map getFummap() {
			return fummap;
		}

		public void setFummap(Map mapcomponent) {
			this.fummap = mapcomponent;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public Map getStringmap() {
			return stringmap;
		}

		public void setStringmap(Map stringmap) {
			this.stringmap = stringmap;
		}

	}

	public MapComponent getMapComponent() {
		return mapComponent;
	}

	public void setMapComponent(MapComponent mapComponent) {
		this.mapComponent = mapComponent;
	}

}
