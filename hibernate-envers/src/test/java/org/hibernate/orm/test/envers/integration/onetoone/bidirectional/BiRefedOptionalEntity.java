/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class BiRefedOptionalEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToOne(mappedBy = "reference", optional = true)
	private BiRefingOptionalEntity referencing;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public BiRefingOptionalEntity getReferencing() {
		return referencing;
	}

	public void setReferencing(BiRefingOptionalEntity referencing) {
		this.referencing = referencing;
	}

	@Override
	public int hashCode() {
		return ( id != null ? id.hashCode() : 0 );
	}

	@Override
	public boolean equals(Object object) {
		if ( object == this ) {
			return true;
		}
		if ( !( object instanceof BiRefedOptionalEntity ) ) {
			return false;
		}
		BiRefedOptionalEntity that = (BiRefedOptionalEntity) object;
		return !( id != null ? !id.equals( that.id ) : that.id != null );
	}
}
