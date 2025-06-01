/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.xml2;

import org.assertj.core.api.Assertions;
import org.hibernate.Hibernate;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel( xmlMappings = {
		"org/hibernate/orm/test/any/xml2/NamedAnyContainerLazy.xml",
		"org/hibernate/orm/test/any/xml2/NamedProperties.xml",
} )
@SessionFactory( generateStatistics = true )
public class AnyLazyXmlTest {
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

			Assertions.assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			Assertions.assertThat( result ).isNotNull();
			Assertions.assertThat( result.getSpecificProperty() ).isNotNull();
			Assertions.assertThat( Hibernate.isInitialized( result.getSpecificProperty() ) ).isFalse();

			Assertions.assertThat( result.getSpecificProperty() ).isInstanceOf( NamedStringProperty.class );
			Assertions.assertThat( result.getSpecificProperty().asString() ).isEqualTo( "Alex" );

			Assertions.assertThat( sqlCollector.getSqlQueries() ).hasSize( 2 );
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

			Assertions.assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );

			Assertions.assertThat( result ).isNotNull();
			Assertions.assertThat( result.getSpecificProperty() ).isNull();

			Assertions.assertThat( result.getGeneralProperties() ).isNotNull();
			Assertions.assertThat( Hibernate.isInitialized( result.getGeneralProperties() ) ).isFalse();
			Assertions.assertThat( result.getGeneralProperties() ).hasSize( 2 );
			Assertions.assertThat( result.getGeneralProperties().stream().map( NamedProperty::getName ) )
					.containsOnly( "name", "age" );

			Assertions.assertThat( sqlCollector.getSqlQueries() ).hasSize( 4 );
		} );
	}
}
