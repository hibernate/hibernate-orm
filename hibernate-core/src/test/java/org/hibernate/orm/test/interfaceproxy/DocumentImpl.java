/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.interfaceproxy;
import java.sql.Blob;
import java.util.Calendar;

/**
 * @author Gavin King
 */
public class DocumentImpl extends ItemImpl implements Document {
	private Blob content;
	private Calendar modified;
	private Calendar created;
	/**
	 * @return Returns the created.
	 */
	public Calendar getCreated() {
		return created;
	}
	/**
	 * @param created The created to set.
	 */
	public void setCreated(Calendar created) {
		this.created = created;
	}
	/**
	 * @return Returns the modified.
	 */
	public Calendar getModified() {
		return modified;
	}
	/**
	 * @param modified The modified to set.
	 */
	public void setModified(Calendar modified) {
		this.modified = modified;
	}
	/**
	 * @return Returns the content.
	 */
	public Blob getContent() {
		return content;
	}
	/**
	 * @param content The content to set.
	 */
	public void setContent(Blob content) {
		this.content = content;
	}
}
