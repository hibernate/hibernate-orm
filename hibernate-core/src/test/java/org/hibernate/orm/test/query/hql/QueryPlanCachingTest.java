/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.math.BigDecimal;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = BasicEntity.class)
@ServiceRegistry
@SessionFactory(exportSchema = true)
public class QueryPlanCachingTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, "entity_1" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testHqlTranslationCaching(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select e from BasicEntity e" ).list();
					session.createQuery( "select e from BasicEntity e" ).list();
				}
		);
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19331")
	public void hhh19331_selectionquery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat(
					session.createSelectionQuery( "select :p0 from BasicEntity", Object[].class )
							.setParameter( "p0", 1 )
							.getSingleResult()
			).containsExactly( 1 );
			assertThat(
					session.createSelectionQuery( "select :p0 from BasicEntity", Object[].class )
							.setParameter( "p0", BigDecimal.valueOf( 3.14 ) )
							.getSingleResult()
			).containsExactly( BigDecimal.valueOf( 3.14 ) );
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-19331")
	public void hhh19331_query(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat(
					session.createQuery( "select :p0 from BasicEntity", Object[].class )
							.setParameter( "p0", 1 )
							.getSingleResult()
			).containsExactly( 1 );
			assertThat(
					session.createQuery( "select :p0 from BasicEntity", Object[].class )
							.setParameter( "p0", BigDecimal.valueOf( 3.14 ) )
							.getSingleResult()
			).containsExactly( BigDecimal.valueOf( 3.14 ) );
		} );
	}
}
