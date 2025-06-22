/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.mutable;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
public class TestEntity {
	@Id
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "t_date")
	private Date date;

	@Basic
	@Column(name = "TEXTS")
	@Convert(converter = MapStringConverter.class)
	private Map<String, String> texts = new HashMap<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Map<String, String> getTexts() {
		return texts;
	}

	public void setTexts(Map<String, String> texts) {
		this.texts = texts;
	}
}
