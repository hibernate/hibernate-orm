/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy;

import org.hibernate.testing.orm.domain.gambit.EntityOfComposites;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = EntityOfComposites.class )
@SessionFactory
public class FromAttributeFactoryTests {
	@Test
	public void simpleTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getRuntimeMetamodels();
	}
}
