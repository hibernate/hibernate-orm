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
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
@MappedSuperclass
public class RevisionMapping implements Serializable {
	private static final long serialVersionUID = 8530213963961662300L;

	@Id
	@GeneratedValue
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

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof RevisionMapping) ) {
			return false;
		}

		final RevisionMapping that = (RevisionMapping) o;
		return id == that.id
				&& timestamp == that.timestamp;
	}

	@Override
	public int hashCode() {
		int result;
		result = id;
		result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
		return result;
	}

	@Override
	public String toString() {
		return "DefaultRevisionEntity(id = " + id
				+ ", revisionDate = " + DateTimeFormatter.INSTANCE.format(getRevisionDate() ) + ")";
	}
}
