/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.onetoone.bidirectional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToOne;

import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class BiRefingOptionalEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@OneToOne(optional = true)
	@JoinTable(name = "A_B", joinColumns = @JoinColumn(name = "a_id", unique = true), inverseJoinColumns = @JoinColumn(name = "b_id") )
	private BiRefedOptionalEntity reference;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public BiRefedOptionalEntity getReference() {
		return reference;
	}

	public void setReference(BiRefedOptionalEntity reference) {
		this.reference = reference;
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
		if ( !( object instanceof BiRefingOptionalEntity ) ) {
			return false;
		}
		BiRefingOptionalEntity that = (BiRefingOptionalEntity) object;
		return !( id != null ? !id.equals( that.id ) : that.id != null );
	}
}
