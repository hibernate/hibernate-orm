/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import java.sql.Types;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.JdbcTypeCode;

/**
 * TextHolder implementation
 *
 * @author Steve Ebersole
 */
@Entity
public class TextHolder {
	@Id
	@GeneratedValue
	private Long id;
	@JdbcTypeCode(Types.LONGVARCHAR)
	@Column(name = "TXT")
	private String theText;

	public TextHolder() {
	}

	public TextHolder(String theText) {
		this.theText = theText;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTheText() {
		return theText;
	}

	public void setTheText(String theText) {
		this.theText = theText;
	}
}
