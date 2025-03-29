/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.hbm;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.any.annotations.IntegerProperty;
import org.hibernate.orm.test.any.annotations.LazyPropertySet;
import org.hibernate.orm.test.any.annotations.Property;
import org.hibernate.orm.test.any.annotations.StringProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@DomainModel(
		annotatedClasses = { StringProperty.class, IntegerProperty.class },
		xmlMappings = "org/hibernate/orm/test/any/hbm/AnyTestLazyPropertySet.hbm.xml"
)
@SessionFactory( generateStatistics = true )
public class AnyLazyHbmTest {

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LazyPropertySet set = new LazyPropertySet( "string" );
					Property property = new StringProperty( "name", "Alex" );
					set.setSomeProperty( property );
					session.persist( set );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete StringProperty" ).executeUpdate();
					session.createQuery( "delete LazyPropertySet" ).executeUpdate();
				}
		);
	}

	@Test
	public void testFetchLazy(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics.isStatisticsEnabled(), is( true ) );
		statistics.clear();

		scope.inTransaction(
				session -> {
					final LazyPropertySet result = session
							.createQuery( "select s from LazyPropertySet s where name = :name", LazyPropertySet.class )
							.setParameter( "name", "string" )
							.getSingleResult();

					assertNotNull( result );
					assertNotNull( result.getSomeProperty() );
					assertThat( Hibernate.isInitialized( result.getSomeProperty() ), is( false ) );

					assertThat( statistics.getPrepareStatementCount(), is(1L ) );

					assertTrue( result.getSomeProperty() instanceof StringProperty );
					assertEquals( "Alex", result.getSomeProperty().asString() );

					assertThat( statistics.getPrepareStatementCount(), is(2L ) );
				}
		);
	}
}
