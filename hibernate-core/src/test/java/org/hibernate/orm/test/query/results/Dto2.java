/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.results;

import org.hibernate.annotations.Imported;

/**
 * @author Steve Ebersole
 */
@Imported
public class Dto2 {
	private final String text;

	public Dto2(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
