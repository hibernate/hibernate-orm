/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Contained.java 4599 2004-09-26 05:18:27Z oneovthafew $
package org.hibernate.test.legacy;
import java.util.ArrayList;
import java.util.Collection;

public class Contained {
	private Container container;
	private long id;
	private Collection bag = new ArrayList();
	private Collection lazyBag = new ArrayList();
	
	public boolean equals(Object other) {
		return id==( (Contained) other ).getId();
	}
	public int hashCode() {
		return new Long(id).hashCode();
	}
	
	/**
	 * Returns the container.
	 * @return Container
	 */
	public Container getContainer() {
		return container;
	}
	
	/**
	 * Returns the id.
	 * @return long
	 */
	public long getId() {
		return id;
	}
	
	/**
	 * Sets the container.
	 * @param container The container to set
	 */
	public void setContainer(Container container) {
		this.container = container;
	}
	
	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(long id) {
		this.id = id;
	}
	
	/**
	 * Returns the bag.
	 * @return Collection
	 */
	public Collection getBag() {
		return bag;
	}
	
	/**
	 * Sets the bag.
	 * @param bag The bag to set
	 */
	public void setBag(Collection bag) {
		this.bag = bag;
	}
	
	/**
	 * Returns the lazyBag.
	 * @return Collection
	 */
	public Collection getLazyBag() {
		return lazyBag;
	}
	
	/**
	 * Sets the lazyBag.
	 * @param lazyBag The lazyBag to set
	 */
	public void setLazyBag(Collection lazyBag) {
		this.lazyBag = lazyBag;
	}
	
}






