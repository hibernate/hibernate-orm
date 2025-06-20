/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import org.hibernate.LazyInitializationException;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedPackageNames = "org.hibernate.orm.test.any.annotations",
		annotatedClasses = {
				StringProperty.class,
				IntegerProperty.class,
				PropertySet.class,
				LazyPropertySet.class,
		}
)
@SessionFactory
@JiraKey( "HHH-13243" )
public class AnyFetchEagerTest {
	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}


	@Test
	public void testManyToAnyFetchEager(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			PropertySet set = new PropertySet( "string" );
			Property property = new StringProperty( "name", "Alex" );
			set.addGeneralProperty( property );
			s.persist( set );
		} );

		PropertySet result = scope.fromTransaction(
				s -> s.createQuery( "select s from PropertySet s where name = :name", PropertySet.class )
						.setParameter( "name", "string" )
						.getSingleResult() );

		assertThat( result ).isNotNull();
		assertThat( result.getGeneralProperties() ).isNotNull();
		assertThat( result.getGeneralProperties().size() ).isEqualTo( 1 );
		assertThat( result.getGeneralProperties().get(0).asString() ).isEqualTo( "Alex" );
	}

	@Test
	public void testManyToAnyFetchLazy(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			LazyPropertySet set = new LazyPropertySet( "string" );
			Property property = new StringProperty( "name", "Alex" );
			set.addGeneralProperty( property );
			s.persist( set );
		} );

		LazyPropertySet result = scope.fromTransaction(
				s -> s.createQuery( "select s from LazyPropertySet s where name = :name", LazyPropertySet.class )
						.setParameter( "name", "string" )
						.getSingleResult() );

		assertThat( result ).isNotNull();
		assertThat( result.getGeneralProperties() ).isNotNull();

		try {
			result.getGeneralProperties().get(0);
			Assertions.fail( "should not get the property string after session closed." );
		}
		catch (LazyInitializationException e) {
			// expected
		}
		catch (Exception e) {
			Assertions.fail( "should not throw exception other than LazyInitializationException." );
		}
	}
}
