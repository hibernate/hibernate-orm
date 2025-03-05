/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;


/**
 * TextHolder implementation
 *
 * @author Steve Ebersole
 */
public class TextHolder {
	private Long id;
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
