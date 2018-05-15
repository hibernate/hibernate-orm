/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Being.java 6007 2005-03-04 12:01:43Z oneovthafew $
package org.hibernate.test.unionsubclass;
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
