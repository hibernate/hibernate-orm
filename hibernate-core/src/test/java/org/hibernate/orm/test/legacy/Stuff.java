/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;
import java.io.Serializable;
import java.util.TimeZone;

public class Stuff implements Serializable {

	public int hashCode() {
		return new Long(id).hashCode();
	}

	public boolean equals(Object other) {
		if ( ! (other instanceof Stuff) ) return false;
		Stuff otherStuff = (Stuff) other;
		return otherStuff.getId()==id && otherStuff.getFoo().getKey().equals( foo.getKey() ) && otherStuff.getMoreStuff().equals(moreStuff);
	}

	private long id;
	private FooProxy foo;
	private MoreStuff moreStuff;
	private TimeZone property;
	/**
	 * Returns the foo.
	 * @return Foo
	 */
	public FooProxy getFoo() {
		return foo;
	}

	/**
	 * Returns the id.
	 * @return long
	 */
	public long getId() {
		return id;
	}

	/**
	 * Returns the property.
	 * @return TimeZone
	 */
	public TimeZone getProperty() {
		return property;
	}

	/**
	 * Sets the foo.
	 * @param foo The foo to set
	 */
	public void setFoo(FooProxy foo) {
		this.foo = foo;
	}

	/**
	 * Sets the id.
	 * @param id The id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Sets the property.
	 * @param property The property to set
	 */
	public void setProperty(TimeZone property) {
		this.property = property;
	}

	/**
	 * Returns the moreStuff.
	 * @return MoreStuff
	 */
	public MoreStuff getMoreStuff() {
		return moreStuff;
	}

	/**
	 * Sets the moreStuff.
	 * @param moreStuff The moreStuff to set
	 */
	public void setMoreStuff(MoreStuff moreStuff) {
		this.moreStuff = moreStuff;
	}

}
