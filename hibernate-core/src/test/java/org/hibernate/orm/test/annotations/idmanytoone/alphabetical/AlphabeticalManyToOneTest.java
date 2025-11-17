/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.idmanytoone.alphabetical;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Acces.class,
				Droitacces.class,
				Benefserv.class,
				Service.class
		}
)
@SessionFactory
public class AlphabeticalManyToOneTest {

	@Test
	public void testAlphabeticalTest(SessionFactoryScope scope) {
		//test through deployment
		scope.inTransaction(
				session -> {
				}
		);
	}
}
