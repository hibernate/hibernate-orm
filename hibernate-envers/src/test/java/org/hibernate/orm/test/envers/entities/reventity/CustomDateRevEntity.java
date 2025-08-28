/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.reventity;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

/**
 * @author Adam Warski (adam at warski dot org)
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
public class CustomDateRevEntity {
	@Id
	@GeneratedValue(generator = "EnversTestingRevisionGenerator")
	@RevisionNumber
	private int customId;

	@RevisionTimestamp
	private Date dateTimestamp;

	public int getCustomId() {
		return customId;
	}

	public void setCustomId(int customId) {
		this.customId = customId;
	}

	public Date getDateTimestamp() {
		return dateTimestamp;
	}

	public void setDateTimestamp(Date dateTimestamp) {
		this.dateTimestamp = dateTimestamp;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		CustomDateRevEntity that = (CustomDateRevEntity) o;

		if ( customId != that.customId ) {
			return false;
		}
		if ( dateTimestamp != null ? !dateTimestamp.equals( that.dateTimestamp ) : that.dateTimestamp != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = customId;
		result = 31 * result + (dateTimestamp != null ? dateTimestamp.hashCode() : 0);
		return result;
	}
}
