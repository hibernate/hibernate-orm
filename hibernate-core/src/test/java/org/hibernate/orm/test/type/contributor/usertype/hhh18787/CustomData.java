/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor.usertype.hhh18787;

/**
 * Simple object holding some properties
 */
public class CustomData {
	private String text;
	private Long number;

	public CustomData(String text, Long number) {
		this.text = text;
		this.number = number;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Long getNumber() {
		return number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}
}
