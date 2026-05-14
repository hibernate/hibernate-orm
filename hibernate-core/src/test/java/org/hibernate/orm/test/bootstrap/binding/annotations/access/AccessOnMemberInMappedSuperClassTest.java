/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.access;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


/**
 * @author Yanming Zhou
 */
@DomainModel(
		annotatedClasses = {
				Apple.class,
				Dog.class
		}
)
@ServiceRegistry
@SessionFactory
public class AccessOnMemberInMappedSuperClassTest {

	@Test
	public void test(SessionFactoryScope scope) {

	}

}
