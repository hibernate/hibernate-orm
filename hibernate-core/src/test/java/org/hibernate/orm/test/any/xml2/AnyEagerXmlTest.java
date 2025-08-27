/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.xml2;

import org.hibernate.Hibernate;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( xmlMappings = {
		"org/hibernate/orm/test/any/xml2/NamedAnyContainerEager.xml",
		"org/hibernate/orm/test/any/xml2/NamedProperties.xml",
} )
@SessionFactory(useCollectingStatementInspector = true)
public class AnyEagerXmlTest {
	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testFetchEagerAny(SessionFactoryScope scope) {
		final SQLStatementInspector sqlCollector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final NamedAnyContainer container = new NamedAnyContainer( 1, "stuff" );
			final NamedProperty property = new NamedStringProperty( 1, "name", "Alex" );
			container.setSpecificProperty( property );
			session.persist( container );
		} );

		scope.inTransaction( (session) -> {
			sqlCollector.clear();
			final NamedAnyContainer result = session
					.createQuery( "from NamedAnyContainer", NamedAnyContainer.class )
					.uniqueResult();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );

			assertThat( result ).isNotNull();
			assertThat( result.getSpecificProperty() ).isNotNull();
			assertThat( Hibernate.isInitialized( result.getSpecificProperty() ) ).isTrue();

			assertThat( result.getSpecificProperty() ).isInstanceOf( NamedStringProperty.class );
			assertThat( result.getSpecificProperty().asString() ).isEqualTo( "Alex" );

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 3 );
		} );
	}

	@Test
	public void testFetchEagerManyToAny(SessionFactoryScope scope) {
		final SQLStatementInspector sqlCollector = scope.getCollectingStatementInspector();

		scope.inTransaction( (session) -> {
			final NamedAnyContainer container = new NamedAnyContainer( 1, "stuff" );
			container.addGeneralProperty( new NamedStringProperty( 1, "name", "Alex" ) );
			container.addGeneralProperty( new NamedIntegerProperty( 1, "age", 23 ) );
			session.persist( container );
		} );

		scope.inTransaction( (session) -> {
			sqlCollector.clear();
			final NamedAnyContainer result = session
					.createQuery( "from NamedAnyContainer", NamedAnyContainer.class )
					.uniqueResult();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 4 );

			assertThat( result ).isNotNull();
			assertThat( result.getSpecificProperty() ).isNull();

			assertThat( result.getGeneralProperties() ).isNotNull();
			assertThat( Hibernate.isInitialized( result.getGeneralProperties() ) ).isTrue();
			assertThat( result.getGeneralProperties() ).hasSize( 2 );
			assertThat( result.getGeneralProperties().stream().map( NamedProperty::getName ) )
					.containsOnly( "name", "age" );

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 4 );
		} );
	}
}
