/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import java.io.Serializable;
import java.util.Date;

/**
 * A base class for a custom revision entity whose revision number is a {@code long},
 * for applications where the number of revisions may exceed {@link Integer#MAX_VALUE}.
 * <p>
 * The default revision entity mappings use an {@code int} revision number, which cannot
 * be changed without breaking existing schemas. Applications that need the larger range
 * can instead declare:
 * <pre>
 * &#64;Entity
 * &#64;RevisionEntity
 * public class MyRevisionEntity extends LongRevisionMapping {
 * }
 * </pre>
 *
 * @see RevisionMapping
 */
@MappedSuperclass
public class LongRevisionMapping implements Serializable {
	private static final long serialVersionUID = 4159156677698841902L;

	@Id
	@GeneratedValue
	@RevisionNumber
	private long id;

	@RevisionTimestamp
	private long timestamp;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Transient
	public Date getRevisionDate() {
		return new Date( timestamp );
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof LongRevisionMapping) ) {
			return false;
		}

		final LongRevisionMapping that = (LongRevisionMapping) o;
		return id == that.id
				&& timestamp == that.timestamp;
	}

	@Override
	public int hashCode() {
		int result;
		result = (int) (id ^ (id >>> 32));
		result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "LongRevisionMapping(id = " + id
				+ ", revisionDate = " + DateTimeFormatter.INSTANCE.format( getRevisionDate() ) + ")";
	}
}
