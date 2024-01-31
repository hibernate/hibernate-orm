/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.sql.exec;

import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.domain.gambit.EmbeddedIdEntity;
import org.hibernate.testing.orm.domain.gambit.EmbeddedIdEntity.EmbeddedIdEntityId;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Chris Cranford
 */
@DomainModel(
		annotatedClasses = {
				EmbeddedIdEntity.class
		}
)
@ServiceRegistry
@SessionFactory(generateStatistics = true)
public class EmbeddedIdEntityTest {

	private EmbeddedIdEntityId entityId;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		final EmbeddedIdEntity entity = new EmbeddedIdEntity();
		entityId = new EmbeddedIdEntityId( 25, "Acme" );
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
				session ->
						session.createQuery( "delete from EmbeddedIdEntity" ).executeUpdate()
		);
	}

	@Test
	public void testHqlSelectAField(SessionFactoryScope scope) {
		StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction(
				session -> {
					final String value = session.createQuery( "select e.data FROM EmbeddedIdEntity e", String.class )
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
					final EmbeddedIdEntity loaded = session.createQuery(
							"select e FROM EmbeddedIdEntity e",
							EmbeddedIdEntity.class
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
					final EmbeddedIdEntityId value = session.createQuery(
							"select e.id FROM EmbeddedIdEntity e",
							EmbeddedIdEntityId.class
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
					final EmbeddedIdEntity loaded = session.get( EmbeddedIdEntity.class, entityId );
					assertThat( loaded, notNullValue() );
					assertThat( loaded.getId(), notNullValue() );
					assertThat( loaded.getId(), equalTo( entityId ) );
					assertThat( loaded.getData(), is( "test" ) );
				}
		);
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17499" )
	public void testNamedParameterComparison(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			final EmbeddedIdEntity loaded = session.createQuery(
					"select e FROM EmbeddedIdEntity e WHERE e.id = :id",
					EmbeddedIdEntity.class
			).setParameter( "id", entityId ).getSingleResult();
			assertThat( loaded.getData(), is( "test" ) );
			assertThat( loaded.getId(), equalTo( entityId ) );
		} );
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17499" )
	public void testPositionalParameterComparison(SessionFactoryScope scope) {
		final StatisticsImplementor statistics = scope.getSessionFactory().getStatistics();
		statistics.clear();
		scope.inTransaction( session -> {
			final EmbeddedIdEntity loaded = session.createQuery(
					"select e FROM EmbeddedIdEntity e WHERE e.id = ?1",
					EmbeddedIdEntity.class
			).setParameter( 1, entityId ).getSingleResult();
			assertThat( loaded.getData(), is( "test" ) );
			assertThat( loaded.getId(), equalTo( entityId ) );
		} );
		assertThat( statistics.getPrepareStatementCount(), is( 1L ) );
	}
}
