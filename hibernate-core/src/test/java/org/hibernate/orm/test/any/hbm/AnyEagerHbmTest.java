/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.any.hbm;

import org.hibernate.Hibernate;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.any.annotations.IntegerProperty;
import org.hibernate.orm.test.any.annotations.Property;
import org.hibernate.orm.test.any.annotations.PropertySet;
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
		xmlMappings = "org/hibernate/orm/test/any/hbm/AnyTestEagerPropertySet.hbm.xml"
)
@SessionFactory( generateStatistics = true )
public class AnyEagerHbmTest {
	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final PropertySet propertySet = new PropertySet( "string" );
					final Property property = new StringProperty( "name", "Alex" );
					propertySet.setSomeProperty( property );
					session.persist( propertySet );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete StringProperty" ).executeUpdate();
					session.createQuery( "delete PropertySet" ).executeUpdate();
				}
		);
	}

	@Test
	public void testFetchEager(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		assertThat( statistics.isStatisticsEnabled(), is( true ) );
		statistics.clear();

		scope.inTransaction(
				session -> {
					final PropertySet result = session
							.createQuery( "from PropertySet", PropertySet.class )
							.uniqueResult();

					assertNotNull( result );
					assertNotNull( result.getSomeProperty() );
					assertThat( Hibernate.isInitialized( result.getSomeProperty() ), is( true ) );

					assertThat( statistics.getPrepareStatementCount(), is(2L ) );

					assertTrue( result.getSomeProperty() instanceof StringProperty );
					assertEquals( "Alex", result.getSomeProperty().asString() );

					assertThat( statistics.getPrepareStatementCount(), is(2L ) );
				}
		);

	}
}
