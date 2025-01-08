/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.usertypeaggregates.model;

import org.hibernate.annotations.Type;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Access(AccessType.FIELD)
@Entity
@Table(name = "CLEARING_QUOTA")
public class ClearingQuota {

	// CONSTRUCTORS
	public ClearingQuota() {
	}

	public ClearingQuota(Long idClearingQuota) {
		this.idClearingQuota = idClearingQuota;
	}

	// PROPERTIES SECTION
	private Long idClearingQuota;
	@Column(name = "QUOTA_CT", nullable = false)
	@Type(DecimalType.class)
	private Decimal quotaCt;
	@Version
	@Column(name = "VERSION")
	private Integer version;

	// GETTERS AND SETTERS SECTION.
	@Id
	@Access(AccessType.PROPERTY)
	@Column(name = "ID_CLEARING_QUOTA")
	@GeneratedValue(generator = "S_CLEARING_QUOTA")
	public Long getIdClearingQuota() {
		return this.idClearingQuota;
	}

	public void setIdClearingQuota(final Long idClearingQuota) {
		this.idClearingQuota = idClearingQuota;
	}

	public Decimal getQuotaCt() {
		return this.quotaCt;
	}

	public void setQuotaCt(final Decimal quotaCt) {
		this.quotaCt = quotaCt;
	}

	public Integer getVersion() {
		return this.version;
	}

	public void setVersion(final Integer version) {
		this.version = version;
	}

	protected Long giveIdValue() {
		return getIdClearingQuota();
	}

}
