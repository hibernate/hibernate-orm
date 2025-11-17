/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.reventity;

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
public class CustomBoxedRevEntity {
	@Id
	@GeneratedValue(generator = "EnversTestingRevisionGenerator")
	@RevisionNumber
	private Integer customId;

	@RevisionTimestamp
	private Long customTimestamp;

	public Integer getCustomId() {
		return customId;
	}

	public void setCustomId(Integer customId) {
		this.customId = customId;
	}

	public Long getCustomTimestamp() {
		return customTimestamp;
	}

	public void setCustomTimestamp(Long customTimestamp) {
		this.customTimestamp = customTimestamp;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof CustomBoxedRevEntity) ) {
			return false;
		}

		CustomBoxedRevEntity that = (CustomBoxedRevEntity) o;

		if ( customId != null ? !customId.equals( that.customId ) : that.customId != null ) {
			return false;
		}
		if ( customTimestamp != null ?
				!customTimestamp.equals( that.customTimestamp ) :
				that.customTimestamp != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (customId != null ? customId.hashCode() : 0);
		result = 31 * result + (customTimestamp != null ? customTimestamp.hashCode() : 0);
		return result;
	}
}
