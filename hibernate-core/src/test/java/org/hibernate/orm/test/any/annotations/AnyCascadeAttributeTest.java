/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				PropertySetWithCascade.class,
				IntegerProperty.class,
				StringProperty.class
		}
)
@SessionFactory
@JiraKey( "HHH-19971" )
public class AnyCascadeAttributeTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testManyToAnyCascadePersist(SessionFactoryScope scope) {
		// Persist parent only, children should cascade
		scope.inTransaction( session -> {
			PropertySetWithCascade set = new PropertySetWithCascade( "test" );
			set.setSomeProperty( new StringProperty( "name", "alex" ) );
			set.addGeneralProperty( new IntegerProperty( "age", 23 ) );
			session.persist( set );

		} );

		// Verify children were persisted via cascade
		scope.inTransaction( session -> {
			PropertySetWithCascade result = session
					.createQuery( "select s from PropertySetWithCascade s where name = :name", PropertySetWithCascade.class )
					.setParameter( "name", "test" )
					.uniqueResult();
			assertNotNull( result );
			assertNotNull( result.getSomeProperty() );
			assertEquals( "alex", result.getSomeProperty().asString() );
			assertEquals( 1, result.getGeneralProperties().size() );
			assertEquals( "23", result.getGeneralProperties().get( 0 ).asString());
		} );
	}

	@Test
	public void testAnyCascadePersist(SessionFactoryScope scope) {
		// Persist parent with @Any child - should cascade
		scope.inTransaction( session -> {

			PropertySetWithCascade set = new PropertySetWithCascade( "any-test" );
			set.setSomeProperty( new IntegerProperty("score", 100) );
			session.persist( set );

		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade result = session
					.createQuery( "select s from PropertySetWithCascade s where name = :name", PropertySetWithCascade.class )
					.setParameter( "name", "any-test" )
					.uniqueResult();
			assertNotNull( result );
			assertNotNull( result.getSomeProperty() );
			assertInstanceOf( IntegerProperty.class, result.getSomeProperty() );
			assertEquals( "100", result.getSomeProperty().asString());
		} );
	}
}
