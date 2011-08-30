//$Id: Location.java 4357 2004-08-17 09:20:17Z oneovthafew $
package org.hibernate.test.unionsubclass;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Gavin King
 */
public class Location {
	private long id;
	private String name;
	private Collection beings = new ArrayList();
	
	Location() {}
	
	public Location(String name) {
		this.name = name;
	}
	
	public void addBeing(Being b) {
		b.setLocation(this);
		beings.add(b);
	}
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
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return Returns the beings.
	 */
	public Collection getBeings() {
		return beings;
	}
	/**
	 * @param beings The beings to set.
	 */
	public void setBeings(Collection beings) {
		this.beings = beings;
	}
}
