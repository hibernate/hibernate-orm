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
public abstract class Customer implements Serializable {
	private PersonID id;

	public PersonID getId() {
		return id;
	}

	public void setId( PersonID id ) {
		this.id = id;
	}

	public boolean equals( Object obj ) {
		if ( obj == null )
			return false;
		if ( obj == this )
			return true;
		if ( !( obj instanceof Customer ) )
			return false;
		return ( (Customer) obj ).getId().equals( getId() );
	}

	public int hashCode() {
		return id.hashCode();
	}

}
