/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.ids;

import java.io.Serializable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@Table(name = "ManyToOneIdNotAud")
public class ManyToOneIdNotAuditedTestEntity implements Serializable {
	@EmbeddedId
	private ManyToOneNotAuditedEmbId id;

	private String data;

	public ManyToOneIdNotAuditedTestEntity() {
	}

	public ManyToOneNotAuditedEmbId getId() {
		return id;
	}

	public void setId(ManyToOneNotAuditedEmbId id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		ManyToOneIdNotAuditedTestEntity that = (ManyToOneIdNotAuditedTestEntity) o;

		if ( data != null ? !data.equals( that.data ) : that.data != null ) {
			return false;
		}
		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (data != null ? data.hashCode() : 0);
		return result;
	}
}
