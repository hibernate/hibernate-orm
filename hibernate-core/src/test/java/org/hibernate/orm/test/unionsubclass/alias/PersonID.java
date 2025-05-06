/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.unionsubclass.alias;
import java.io.Serializable;

/**
 *
 * @author Strong Liu
 */
public class PersonID implements Serializable {
	private Long num;
	private String name;

	public Long getNum() {
		return num;
	}

	public void setNum( Long num ) {
		this.num = num;
	}

	public String getName() {
		return name;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public boolean equals( Object obj ) {
		if ( this == obj )
			return true;
		if ( obj == null )
			return false;
		if ( getClass() != obj.getClass() )
			return false;
		final PersonID other = (PersonID) obj;
		if ( name == null ) {
			if ( other.name != null )
				return false;

		} else if ( !name.equals( other.name ) ) {
			return false;
		}
		if ( num == null ) {
			if ( other.num != null )
				return false;

		} else if ( !num.equals( other.num ) ) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		if ( name != null ) {
			result += name.hashCode();
		}
		result *= PRIME;
		if ( num != null ) {
			result += num.hashCode();
		}
		return result;
	}

	public String toString() {
		return name + " | " + num;
	}

}
