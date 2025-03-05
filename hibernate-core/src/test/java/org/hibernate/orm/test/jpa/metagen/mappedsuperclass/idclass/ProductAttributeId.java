/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metagen.mappedsuperclass.idclass;

/**
 * @author Alexis Bataille
 * @author Steve Ebersole
 */
public class ProductAttributeId extends AbstractAttributeId {
	private String owner;

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key == null) ? 0 : key.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		ProductAttributeId other = (ProductAttributeId) obj;
		if ( key == null ) {
			if ( other.getKey() != null ) {
				return false;
			}
		}
		else if ( !key.equals( other.getKey() ) ) {
			return false;
		}
		if ( owner == null ) {
			if ( other.getOwner() != null ) {
				return false;
			}
		}
		else if ( !owner.equals( other.getOwner() ) ) {
			return false;
		}
		return true;
	}
}
