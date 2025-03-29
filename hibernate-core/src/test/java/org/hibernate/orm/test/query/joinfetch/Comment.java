/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.joinfetch;

import java.util.Calendar;

/**
 * @author Gavin King
 */
public class Comment {

	private String text;
	private Item item;
	private Calendar timestamp;
	private Long id;

	public Item getItem() {
		return item;
	}
	public void setItem(Item item) {
		this.item = item;
	}
	public Calendar getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Calendar timestamp) {
		this.timestamp = timestamp;
	}

	Comment() {}
	public Comment(Item item, String comment) {
		this.text = comment;
		this.item = item;
		item.getComments().add(this);
		this.timestamp = Calendar.getInstance();
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}

}
