/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.audit;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

import org.hibernate.annotations.RevisionEntity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Date;

/**
 * Base {@link MappedSuperclass @MappedSuperclass} for revision
 * entities, providing the standard {@code REV} (auto-generated
 * integer primary key) and {@code REVTSTMP} (Unix epoch
 * timestamp) columns.
 * <p>
 * Extend this class to create a custom
 * {@link RevisionEntity @RevisionEntity}. For entity change
 * tracking, extend {@link TrackingModifiedEntitiesRevisionMapping}
 * instead.
 *
 * @author Marco Belladelli
 * @see DefaultRevisionEntity
 * @see TrackingModifiedEntitiesRevisionMapping
 * @since 7.4
 */
@MappedSuperclass
public class RevisionMapping implements Serializable {
	@Id
	@GeneratedValue
	@RevisionEntity.TransactionId
	@Column(name = "REV")
	private int id;

	@RevisionEntity.Timestamp
	@Column(name = "REVTSTMP")
	private long timestamp;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @deprecated Use {@link #getRevisionInstant()}
	 */
	@Deprecated
	@Transient
	public Date getRevisionDate() {
		return new Date( timestamp );
	}

	@Transient
	public Instant getRevisionInstant() {
		return Instant.ofEpochMilli( timestamp );
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
		if ( !( o instanceof RevisionMapping that ) ) {
			return false;
		}
		return id == that.id
				&& timestamp == that.timestamp;
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + Long.hashCode( timestamp );
		return result;
	}

	@Override
	public String toString() {
		return "RevisionMapping(id = " + id
				+ ", timestamp = " + getRevisionInstant() + ")";
	}
}
