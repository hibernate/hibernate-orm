/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.exec;

import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EntityWithNotAggregateId;
import org.hibernate.testing.orm.domain.gambit.EntityWithNotAggregateId.PK;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

@DomainModel(
		annotatedClasses = {
				EntityWithNotAggregateId.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
@Disabled(value = "non aggregate composit id has not been yet implemented")
public class EntityWithNotAggregateIdTest {

	private PK entityId;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		final EntityWithNotAggregateId entity = new EntityWithNotAggregateId();
		entityId = new PK( 25, "Acme" );
		scope.inTransaction(
				session -> {
					entity.setId( entityId );
					entity.setData( "test" );
					session.save( entity );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				sesison ->
						sesison.createQuery( "delete from EntityWithIdClass" ).executeUpdate()
		);
	}

	@Test
	public void testHqlSelectAField(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery( "select e.data FROM EntityWithIdClass e", String.class )
							.uniqueResult();
					assertThat( value, is( "test" ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	public void testHqlSelect(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithNotAggregateId loaded = session.createQuery(
							"select e FROM EntityWithIdClass e",
							EntityWithNotAggregateId.class
					).uniqueResult();
					assertThat( loaded.getData(), is( "test" ) );
					assertThat( loaded.getId(), equalTo( entityId ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	public void testHqlSelectOnlyTheEmbeddedId(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithNotAggregateId value = session.createQuery(
							"select e.id FROM EntityWithIdClass e",
							EntityWithNotAggregateId.class
					).uniqueResult();
					assertThat( value, equalTo( entityId ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final EntityWithNotAggregateId loaded = session.get( EntityWithNotAggregateId.class, entityId );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getId(), notNullValue() );
					assertThat( loaded.getId(), equalTo( entityId ) );
					assertThat( loaded.getData(), is( "test" ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}
}
