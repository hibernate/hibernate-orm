/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.index.jpa;

/**
 * @author Strong Liu
 */
public class IndexTest extends AbstractJPAIndexTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Car.class,
				Dealer.class,
				Importer.class
		};
	}
}
