/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import in.from.Any;

@DomainModel( annotatedClasses = Any.class )
@SessionFactory
@JiraKey( "HHH-11784" )
public class FunnyNamesTests {
	@Test
	public void basicTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Any" ).list();
			session.createQuery( "from in.from.Any" ).list();
		} );
	}
}
