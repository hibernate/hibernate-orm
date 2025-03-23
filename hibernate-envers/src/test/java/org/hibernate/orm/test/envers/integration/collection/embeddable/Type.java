/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.embeddable;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.Immutable;

/**
 * @author Chris Cranford
 */
@Entity
@Immutable
public class Type {
	@Id
	private Integer id;
	private String name;

	Type() {

	}

	Type(Integer id) {
		this( id, null );
	}

	Type(Integer id, String name) {
		this.id = id;
		this.name = name;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( name != null ? name.hashCode() : 0 );
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		if ( object == null || getClass() != object.getClass() ) {
			return false;
		}

		Type that = (Type) object;
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		return !( name != null ? !name.equals( that.name ) : that.name != null );
	}
}
