/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.results;

import jakarta.persistence.Embeddable;

/**
 * @author Steve Ebersole
 */
@Embeddable
public class SimpleComposite {
	public String value1;
	public String value2;

	public SimpleComposite() {
	}

	public SimpleComposite(String value1, String value2) {
		this.value1 = value1;
		this.value2 = value2;
	}
}
