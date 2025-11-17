/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;
import java.util.Date;

/**
 * @author Gavin King
 */
public class Document {

	private String text;
	private String name;
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
