/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.integrationtest.java.module.test.annotation;

import jakarta.persistence.Embeddable;

@Embeddable
public class StubEmbeddable {
	public String value;
}
