/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter.registrations;

import java.io.Serializable;

/**
 * @author Steve Ebersole
 */
public class Thing1 implements Thing, Serializable {
	private final String content;

	public Thing1(String content) {
		this.content = content;
	}

	@Override
	public String getContent() {
		return content;
	}
}
