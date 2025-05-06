/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.reventity;

import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * @author Chris Cranford
 */
@Entity
@GenericGenerator(name = "EnversTestingRevisionGenerator",
		strategy = "org.hibernate.id.enhanced.TableGenerator",
		parameters = {
				@Parameter(name = "table_name", value = "REVISION_GENERATOR"),
				@Parameter(name = "initial_value", value = "1"),
				@Parameter(name = "increment_size", value = "1"),
				@Parameter(name = "prefer_entity_table_as_segment_value", value = "true")
		}
)
@RevisionEntity
public class CustomInstantRevEntity {
	@Id
	@GeneratedValue(generator = "EnversTestingRevisionGenerator")
	@RevisionNumber
	private int customId;

	@RevisionTimestamp
	private Instant instantTimestamp;

	public int getCustomId() {
		return customId;
	}

	public void setCustomId(int customId) {
		this.customId = customId;
	}

	public Instant getInstantTimestamp() {
		return instantTimestamp;
	}

	public void setInstantTimestamp(Instant instantTimestamp) {
		this.instantTimestamp = instantTimestamp;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CustomInstantRevEntity that = (CustomInstantRevEntity) o;

		if ( customId != that.customId ) {
			return false;
		}
		if ( instantTimestamp != null ? !instantTimestamp.equals( that.instantTimestamp ) : that.instantTimestamp != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = customId;
		result = 31 * result + (instantTimestamp != null ? instantTimestamp.hashCode() : 0);
		return result;
	}
}
