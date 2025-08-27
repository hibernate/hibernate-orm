/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.ids;

import java.io.Serializable;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;

import org.hibernate.orm.test.envers.entities.UnversionedStrTestEntity;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Embeddable
public class ManyToOneNotAuditedEmbId implements Serializable {
	@ManyToOne(optional = false)
	private UnversionedStrTestEntity id;

	public ManyToOneNotAuditedEmbId() {
	}

	public ManyToOneNotAuditedEmbId(UnversionedStrTestEntity id) {
		this.id = id;
	}

	public UnversionedStrTestEntity getId() {
		return id;
	}

	public void setId(UnversionedStrTestEntity id) {
		this.id = id;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ManyToOneNotAuditedEmbId that = (ManyToOneNotAuditedEmbId) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}
