/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;


/**
 * @author Gavin King
 */
@Entity
public class Document {

	private String text;
	@Id
	private String name;
	@Version
	@Column(name = "last_modified")
	private Date lastModified;

	Document() {}

	public Document(String text, String name) {
		this.text = text;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Date getLastModified() {
		return lastModified;
	}

	void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

}
