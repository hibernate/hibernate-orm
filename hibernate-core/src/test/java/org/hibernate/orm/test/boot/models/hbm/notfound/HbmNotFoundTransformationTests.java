/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.notfound;

import org.hibernate.orm.test.unconstrained.UnconstrainedTest;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @see UnconstrainedTest
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "mappings/models/hbm/notfound/mapping.xml")
@SessionFactory
public class HbmNotFoundTransformationTests {
	@Test
	void testNotFound(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Employee2 employee = new Employee2( 1, "employee" );
			final Person2 person = new Person2( 1, "person", employee );
			session.persist( employee );
			session.persist( person );
		} );
		scope.inTransaction( (session) -> session.createMutationQuery( "delete Employee2" ).executeUpdate() );
		scope.inTransaction( (session) -> {
			final Person2 loaded = session.find( Person2.class, 1 );
			assertThat( loaded ).isNotNull();
			assertThat( loaded.getEmployee() ).isNull();
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.dropData();
	}
}
