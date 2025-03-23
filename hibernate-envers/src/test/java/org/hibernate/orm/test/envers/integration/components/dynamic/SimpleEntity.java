/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.components.dynamic;

import org.hibernate.envers.Audited;

@Audited
public class SimpleEntity {

	private Long id;
	private String simpleProperty;

	private AdvancedEntity parent;

	public SimpleEntity() {
	}

	public SimpleEntity(Long id, String simpleProperty) {
		this.id = id;
		this.simpleProperty = simpleProperty;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSimpleProperty() {
		return simpleProperty;
	}

	public void setSimpleProperty(String simpleProperty) {
		this.simpleProperty = simpleProperty;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof SimpleEntity that ) ) {
			return false;
		}

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( simpleProperty != null ? !simpleProperty.equals( that.simpleProperty ) : that.simpleProperty != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + ( simpleProperty != null ? simpleProperty.hashCode() : 0 );
		return result;
	}

	@Override
	public String toString() {
		return "SimpleEntity{" +
				"id=" + id +
				", simpleProperty='" + simpleProperty + '\'' +
				'}';
	}

	public AdvancedEntity getParent() {
		return parent;
	}

	public void setParent(AdvancedEntity parent) {
		this.parent = parent;
	}
}
