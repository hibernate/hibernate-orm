/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.generated;

import org.hibernate.annotations.Generated;
import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class SimpleEntity {
	@Id
	@GeneratedValue
	private Integer id;

	private String data;

	@Generated
	@Column(columnDefinition = "integer default 1")
	private int caseNumberInsert;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public int getCaseNumberInsert() {
		return caseNumberInsert;
	}

	public void setCaseNumberInsert(int caseNumberInsert) {
		this.caseNumberInsert = caseNumberInsert;
	}
}
