/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.enhanced;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.DateTimeFormatter;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
@MappedSuperclass
public class SequenceIdRevisionMapping implements Serializable {
	private static final long serialVersionUID = 4159156677698841902L;

	@Id
	@GeneratedValue(generator = "RevisionNumberSequenceGenerator")
	@GenericGenerator(
			name = "RevisionNumberSequenceGenerator",
			strategy = "org.hibernate.envers.enhanced.OrderedSequenceGenerator",
			parameters = {
					@Parameter(name = "table_name", value = "REVISION_GENERATOR"),
					@Parameter(name = "sequence_name", value = "REVISION_GENERATOR"),
					@Parameter(name = "initial_value", value = "1"),
					@Parameter(name = "increment_size", value = "1")
			}
	)
	@RevisionNumber
	private int id;

	@RevisionTimestamp
	private long timestamp;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Transient
	public Date getRevisionDate() {
		return new Date( timestamp );
	}

	public long getTimestamp() {
		return timestamp;
	}

	@SuppressWarnings("unused")
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof SequenceIdRevisionMapping) ) {
			return false;
		}

		final SequenceIdRevisionMapping that = (SequenceIdRevisionMapping) o;
		return id == that.id && timestamp == that.timestamp;
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "SequenceIdRevisionEntity(id = " + id
				+ ", revisionDate = " + DateTimeFormatter.INSTANCE.format(getRevisionDate() )
				+ ")";
	}
}
