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
@RevisionEntity(TestRevisionListener.class)
public class ListenerRevEntity {
	@Id
	@GeneratedValue(generator = "EnversTestingRevisionGenerator")
	@RevisionNumber
	private int id;

	@RevisionTimestamp
	private long timestamp;

	private String data;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ListenerRevEntity) ) {
			return false;
		}

		ListenerRevEntity revEntity = (ListenerRevEntity) o;

		if ( id != revEntity.id ) {
			return false;
		}
		if ( timestamp != revEntity.timestamp ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = id;
		result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}
}
