/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

/**
 * Flight
 *
 * @author Emmanuel Bernard
 */
@Entity()
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Flight implements Serializable {
	Long id;
	String name;
	transient Long duration;
	long durationInSec;
	Integer version;
	Company company;
	String triggeredData;
	long factor;
	Date departureDate;
	java.sql.Timestamp buyDate;
	Calendar alternativeDepartureDate;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}

	@Column(name = "flight_name", nullable = false, updatable = false, length = 50)
	public String getName() {
		return name;
	}

	public void setName(String string) {
		name = string;
	}

	@Basic(fetch = FetchType.LAZY, optional = false)
	public Long getDuration() {
		return duration;
	}

	@Basic
	@Temporal(TemporalType.DATE)
	public Date getDepartureDate() {
		return departureDate;
	}

	public void setDepartureDate(Date departureDate) {
		this.departureDate = departureDate;
	}


	public void setDuration(Long l) {
		duration = l;
		//durationInSec = duration / 1000;
	}

	@Transient
	public long getDurationInSec() {
		return durationInSec;
	}

	public void setDurationInSec(long l) {
		durationInSec = l;
	}

	@Version
	@Column(name = "OPTLOCK")
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer i) {
		version = i;
	}

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinColumn(name = "COMP_ID")
	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	@Column(insertable = false, updatable = false)
	public String getTriggeredData() {
		return triggeredData;
	}

	public void setTriggeredData(String string) {
		triggeredData = string;
	}

	public void getIsNotAGetter() {
		//do nothing
	}

	public long getFactor(boolean x10) {
		//this is not a getter should not be persisted
		return factor * ( 1 + ( x10 == true ? 9 : 0 ) );
	}

	public void setFactor(long factor) {
		this.factor = factor;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Calendar getAlternativeDepartureDate() {
		return alternativeDepartureDate;
	}

	public void setAlternativeDepartureDate(Calendar alternativeDepartureDate) {
		this.alternativeDepartureDate = alternativeDepartureDate;
	}

	public java.sql.Timestamp getBuyDate() {
		return buyDate;
	}

	public void setBuyDate(java.sql.Timestamp buyDate) {
		this.buyDate = buyDate;
	}

}
