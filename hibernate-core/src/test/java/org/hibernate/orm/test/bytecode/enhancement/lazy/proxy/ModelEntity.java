/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.sql.Timestamp;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
@MappedSuperclass
public abstract class ModelEntity {

	@Id
	@Column(name="Oid")
	private Long Oid = null;

	@Basic
	@Column(name="CreatedAt")
	private Timestamp CreatedAt = null;

	@Basic
	@Column(name="CreatedBy")
	private String CreatedBy = null;

	@Basic
	@Column(name="VersionNr")
	private short VersionNr = 0;

	public short getVersionNr() {
		return VersionNr;
	}

	public void setVersionNr(short versionNr) {
		this.VersionNr = versionNr;
	}

	public Long getOid() {
		return Oid;
	}

	public void setOid(Long oid) {
		this.Oid = oid;
	}

	public Timestamp getCreatedAt() {
		return CreatedAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.CreatedAt = createdAt;
	}

	public String getCreatedBy() {
		return CreatedBy;
	}

	public void setCreatedBy(String createdBy) {
		this.CreatedBy = createdBy;
	}

}
