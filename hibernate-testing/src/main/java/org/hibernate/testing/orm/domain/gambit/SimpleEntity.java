/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.gambit;

import java.time.Instant;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import org.hibernate.annotations.NaturalId;

/**
 * @author Steve Ebersole
 */
@Entity
@Table(name = "SIMPLE_ENTITY")
public class SimpleEntity {
	private Integer id;

	// NOTE : alphabetical
	private Date someDate;
	private Instant someInstant;
	private Integer someInteger;
	private Long someLong;
	private String someString;

	public SimpleEntity() {
	}

	public SimpleEntity(
			Integer id,
			String someString) {
		this.id = id;
		this.someString = someString;
	}

	public SimpleEntity(
			Integer id,
			String someString,
			Long someLong) {
		this.id = id;
		this.someString = someString;
		this.someLong = someLong;
	}

	public SimpleEntity(
			Integer id,
			String someString,
			Long someLong,
			Integer someInteger) {
		this.id = id;
		this.someString = someString;
		this.someLong = someLong;
		this.someInteger = someInteger;
	}

	public SimpleEntity(
			Integer id,
			Date someDate,
			Instant someInstant,
			Integer someInteger,
			Long someLong,
			String someString) {
		this.id = id;
		this.someDate = someDate;
		this.someInstant = someInstant;
		this.someInteger = someInteger;
		this.someLong = someLong;
		this.someString = someString;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSomeString() {
		return someString;
	}

	public void setSomeString(String someString) {
		this.someString = someString;
	}

	@NaturalId
	public Integer getSomeInteger() {
		return someInteger;
	}

	public void setSomeInteger(Integer someInteger) {
		this.someInteger = someInteger;
	}

	public Long getSomeLong() {
		return someLong;
	}

	public void setSomeLong(Long someLong) {
		this.someLong = someLong;
	}

	@Temporal( TemporalType.TIMESTAMP )
	public Date getSomeDate() {
		return someDate;
	}

	public void setSomeDate(Date someDate) {
		this.someDate = someDate;
	}

	public Instant getSomeInstant() {
		return someInstant;
	}

	public void setSomeInstant(Instant someInstant) {
		this.someInstant = someInstant;
	}
}
