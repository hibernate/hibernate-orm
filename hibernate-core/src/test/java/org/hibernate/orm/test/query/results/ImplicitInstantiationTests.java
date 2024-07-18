/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.query.results;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {SimpleEntity.class, SimpleComposite.class, Dto.class, Dto2.class})
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class ImplicitInstantiationTests {
	@BeforeEach
	void prepareTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.persist( new SimpleEntity( 1, "first", new SimpleComposite( "value1", "value2" ) ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.createMutationQuery( "delete SimpleEntity" ).executeUpdate();
		});
	}

	@Test
	void testCreateQuery(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createQuery( Queries.ID_NAME, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createQuery( Queries.ID_COMP_VAL, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "value1" );
		} );
	}

	@Test
	void testCreateSelectionQuery(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createSelectionQuery( Queries.ID_NAME, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createSelectionQuery( Queries.ID_COMP_VAL, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "value1" );
		} );
	}

	@Test
	void testCriteria(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

			final JpaCriteriaQuery<Dto> criteria = criteriaBuilder.createQuery( Dto.class );
			final JpaRoot<SimpleEntity> root = criteria.from( SimpleEntity.class );
			criteria.multiselect( root.get( "id" ), root.get( "name" ) );
			final Dto rtn = session.createQuery( criteria ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18306" )
	void testCreateQuerySingleSelectItem(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Dto2 rtn = session.createQuery( Queries.NAME, Dto2.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto2 rtn = session.createQuery( Queries.COMP_VAL, Dto2.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getText() ).isEqualTo( "value1" );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18306" )
	void testCreateSelectionQuerySingleSelectItem(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final Dto2 rtn = session.createSelectionQuery( Queries.NAME, Dto2.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto2 rtn = session.createSelectionQuery( Queries.COMP_VAL, Dto2.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getText() ).isEqualTo( "value1" );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18306" )
	void testCriteriaSingleSelectItem(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

			final JpaCriteriaQuery<Dto2> criteria = criteriaBuilder.createQuery( Dto2.class );
			final JpaRoot<SimpleEntity> root = criteria.from( SimpleEntity.class );
			criteria.multiselect( root.get( "name" ) );
			final Dto2 rtn = session.createQuery( criteria ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );
	}
}
