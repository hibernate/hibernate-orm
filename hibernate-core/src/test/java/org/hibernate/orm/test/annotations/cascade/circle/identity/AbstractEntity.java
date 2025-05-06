/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade.circle.identity;

import java.io.Serializable;
import java.util.Date;

import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SequenceGenerator;

@MappedSuperclass
public abstract class AbstractEntity implements Serializable {

	@Id
	@SequenceGenerator(name = "TIGER_GEN", sequenceName = "TIGER_SEQ")
	@GeneratedValue(strategy = GenerationType.IDENTITY, generator = "TIGER_GEN")
	private Long id;
	@Basic
	@Column(unique = true, updatable = false, length = 36, columnDefinition = "char(36)")
	private String uuid;
	@Column(updatable = false)
	private Date created;

	public AbstractEntity() {
		super();
		uuid = SafeRandomUUIDGenerator.safeRandomUUIDAsString();
		created = new Date();
	}

	public Long getId() {
		return id;
	}

	public String getUuid() {
		return uuid;
	}

	public Date getCreated() {
		return created;
	}

	@Override
	public int hashCode() {
		return uuid == null ? 0 : uuid.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof AbstractEntity ))
			return false;
		final AbstractEntity other = (AbstractEntity) obj;
		if (uuid == null) {
			if (other.uuid != null)
				return false;
		} else if (!uuid.equals(other.uuid))
			return false;
		return true;
	}

	public String toString() {
		if (id != null) {
			return "id: '" + id + "' uuid: '" + uuid + "'";
		} else {
			return "id: 'transient entity' " + " uuid: '" + uuid + "'";
		}
	}
}
