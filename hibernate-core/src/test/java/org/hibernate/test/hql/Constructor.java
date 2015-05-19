/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.io.Serializable;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class Constructor implements Serializable {
	private static int CONSTRUCTOR_EXECUTION_COUNT = 0;

	private long id;
	private String someString;
	private Number someNumber;
	private boolean someBoolean;
	private boolean anotherBoolean;

	public Constructor() {
	}

	public Constructor(long id, boolean someBoolean, boolean anotherBoolean, Number someNumber, String someString) {
		this.id = id;
		this.someBoolean = someBoolean;
		this.anotherBoolean = anotherBoolean;
		this.someNumber = someNumber;
		this.someString = someString;
		++CONSTRUCTOR_EXECUTION_COUNT;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) return true;
		if ( !( o instanceof Constructor ) ) return false;

		Constructor that = (Constructor) o;

		if ( anotherBoolean != that.anotherBoolean ) return false;
		if ( id != that.id ) return false;
		if ( someBoolean != that.someBoolean ) return false;
		if ( someNumber != null ? !someNumber.equals( that.someNumber ) : that.someNumber != null ) return false;
		if ( someString != null ? !someString.equals( that.someString ) : that.someString != null ) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (id ^ (id >>> 32));
		result = 31 * result + (someString != null ? someString.hashCode() : 0);
		result = 31 * result + (someNumber != null ? someNumber.hashCode() : 0);
		result = 31 * result + (someBoolean ? 1 : 0);
		result = 31 * result + (anotherBoolean ? 1 : 0);
		return result;
	}

	public boolean isSomeBoolean() {
		return someBoolean;
	}

	public void setSomeBoolean(boolean someBoolean) {
		this.someBoolean = someBoolean;
	}

	public Number getSomeNumber() {
		return someNumber;
	}

	public void setSomeNumber(Number someNumber) {
		this.someNumber = someNumber;
	}

	public String getSomeString() {
		return someString;
	}

	public void setSomeString(String someString) {
		this.someString = someString;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public boolean isAnotherBoolean() {
		return anotherBoolean;
	}

	public void setAnotherBoolean(boolean anotherBoolean) {
		this.anotherBoolean = anotherBoolean;
	}

	public static int getConstructorExecutionCount() {
		return CONSTRUCTOR_EXECUTION_COUNT;
	}

	public static void resetConstructorExecutionCount() {
		CONSTRUCTOR_EXECUTION_COUNT = 0;
	}
}
