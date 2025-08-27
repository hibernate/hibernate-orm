/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.UUID;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = EntityOfBasics.class)
@SessionFactory
public class UuidAggregationTest {
	@Test
	@JiraKey(value = "HHH-15495")
	public void testMaxUuid(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createSelectionQuery( "select max(theUuid) from EntityOfBasics", UUID.class ).getSingleResult();
		} );
	}
}
