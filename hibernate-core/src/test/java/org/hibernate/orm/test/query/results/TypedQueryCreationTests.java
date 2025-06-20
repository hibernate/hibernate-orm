/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.results;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.junit.DomainModel;
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
@Jira( "https://hibernate.atlassian.net/browse/HHH-18401" )
@DomainModel(annotatedClasses = {SimpleEntity.class, SimpleComposite.class, Dto.class, Dto2.class})
@SessionFactory
@SuppressWarnings("JUnitMalformedDeclaration")
public class TypedQueryCreationTests {
	@BeforeEach
	void prepareTestData(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			session.persist( new SimpleEntity( 1, "first", new SimpleComposite( "value1", "value2" ) ) );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testCreateQuery(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final SimpleEntity rtn = session.createQuery( Queries.ENTITY, SimpleEntity.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.id ).isEqualTo( 1 );
		} );

		sessions.inTransaction( (session) -> {
			final SimpleEntity rtn = session.createQuery( Queries.ENTITY_NO_SELECT, SimpleEntity.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.id ).isEqualTo( 1 );
		} );

		sessions.inTransaction( (session) -> {
			final SimpleComposite rtn = session.createQuery( Queries.COMPOSITE, SimpleComposite.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.value1 ).isEqualTo( "value1" );
			assertThat( rtn.value2 ).isEqualTo( "value2" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createQuery( Queries.ID_NAME_DTO, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createQuery( Queries.ID_COMP_VAL_DTO, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "value1" );
		} );

		sessions.inTransaction( (session) -> {
			final String rtn = session.createQuery( Queries.NAME, String.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final String rtn = session.createQuery( Queries.COMP_VAL, String.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn ).isEqualTo( "value1" );
		} );
	}

	@Test
	void testCreateSelectionQuery(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final SimpleEntity rtn = session.createSelectionQuery( Queries.ENTITY, SimpleEntity.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.id ).isEqualTo( 1 );
		} );

		sessions.inTransaction( (session) -> {
			final SimpleEntity rtn = session.createSelectionQuery( Queries.ENTITY_NO_SELECT, SimpleEntity.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.id ).isEqualTo( 1 );
		} );

		sessions.inTransaction( (session) -> {
			final SimpleComposite rtn = session.createSelectionQuery( Queries.COMPOSITE, SimpleComposite.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.value1 ).isEqualTo( "value1" );
			assertThat( rtn.value2 ).isEqualTo( "value2" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createSelectionQuery( Queries.ID_NAME_DTO, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createSelectionQuery( Queries.ID_COMP_VAL_DTO, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "value1" );
		} );

		sessions.inTransaction( (session) -> {
			final String rtn = session.createSelectionQuery( Queries.NAME, String.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final String rtn = session.createSelectionQuery( Queries.COMP_VAL, String.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn ).isEqualTo( "value1" );
		} );
	}

	@Test
	void testCreateNamedQuery(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final SimpleEntity rtn = session.createNamedQuery( Queries.NAMED_ENTITY, SimpleEntity.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.id ).isEqualTo( 1 );
		} );

		sessions.inTransaction( (session) -> {
			final SimpleEntity rtn = session.createNamedQuery( Queries.NAMED_ENTITY_NO_SELECT, SimpleEntity.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.id ).isEqualTo( 1 );
		} );

		sessions.inTransaction( (session) -> {
			final SimpleComposite rtn = session.createNamedQuery( Queries.NAMED_COMPOSITE, SimpleComposite.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.value1 ).isEqualTo( "value1" );
			assertThat( rtn.value2 ).isEqualTo( "value2" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createNamedQuery( Queries.NAMED_ID_NAME_DTO, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final Dto rtn = session.createNamedQuery( Queries.NAMED_ID_COMP_VAL_DTO, Dto.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.getKey() ).isEqualTo( 1 );
			assertThat( rtn.getText() ).isEqualTo( "value1" );
		} );

		sessions.inTransaction( (session) -> {
			final String rtn = session.createNamedQuery( Queries.NAMED_NAME, String.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn ).isEqualTo( "first" );
		} );

		sessions.inTransaction( (session) -> {
			final String rtn = session.createNamedQuery( Queries.NAMED_COMP_VAL, String.class ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn ).isEqualTo( "value1" );
		} );
	}

	@Test
	void testCriteria(SessionFactoryScope sessions) {
		sessions.inTransaction( (session) -> {
			final HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();

			final JpaCriteriaQuery<SimpleEntity> criteria = criteriaBuilder.createQuery( SimpleEntity.class );
			final JpaRoot<SimpleEntity> root = criteria.from( SimpleEntity.class );
			criteria.select( root );
			final SimpleEntity rtn = session.createQuery( criteria ).getSingleResultOrNull();
			assertThat( rtn ).isNotNull();
			assertThat( rtn.id ).isEqualTo( 1 );
		} );
	}
}
