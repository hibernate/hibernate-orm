/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.usertypeaggregates.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Type;

@Access(AccessType.FIELD)
@Entity
@Table(name = "CLEARING_QUOTA")
@NamedQuery(name = ClearingQuotaWithNamedQuery.SELECT_SUM,
		query = ClearingQuotaWithNamedQuery.SELECT_SUM_QUERY)
@NamedQuery(name = ClearingQuotaWithNamedQuery.SELECT_AVERAGE,
		query = ClearingQuotaWithNamedQuery.SELECT_AVERAGE_QUERY)
public class ClearingQuotaWithNamedQuery {

	public static final String SELECT_SUM = "select_sum";
	public static final String SELECT_AVERAGE = "select_average";
	public static final String SELECT_SUM_QUERY = "SELECT sum(c.quotaCt) FROM ClearingQuotaWithNamedQuery c";
	public static final String SELECT_AVERAGE_QUERY = "SELECT avg(c.quotaCt) FROM ClearingQuotaWithNamedQuery c";

	// CONSTRUCTORS
	public ClearingQuotaWithNamedQuery() {
	}

	public ClearingQuotaWithNamedQuery(Long idClearingQuota) {
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
