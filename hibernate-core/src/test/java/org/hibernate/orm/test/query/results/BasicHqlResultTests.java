/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.results;

import java.util.List;
import jakarta.persistence.Tuple;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = {SimpleEntity.class, Dto.class, Dto2.class } )
@SessionFactory
public class BasicHqlResultTests {
	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new SimpleEntity( 1, "first", new SimpleComposite( "a", "b" ) ) );
		});
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testBasicSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "select id from SimpleEntity order by id", Integer.class ).list();
		});
	}

	@Test
	public void testBasicTupleSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<Tuple> tuples = session
					.createQuery( "select id as id, name as name from SimpleEntity order by id, name", Tuple.class )
					.list();
			assertThat( tuples ).hasSize( 1 );

			final Tuple result = tuples.get( 0 );
			assertThat( result.get( 0 ) ).isEqualTo( 1 );
			assertThat( result.get( "id" ) ).isEqualTo( 1 );
			assertThat( result.get( 1 ) ).isEqualTo( "first" );
			assertThat( result.get( "name" ) ).isEqualTo( "first" );
		});
	}

	@Test
	public void testCompositeSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<SimpleComposite> list = session
					.createQuery( "select composite from SimpleEntity order by composite", SimpleComposite.class )
					.list();
		});
	}

	@Test
	public void testBasicAndCompositeTuple(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String qry = "select id as id, composite as composite from SimpleEntity order by name";
			final List<Tuple> list = session.createQuery( qry, Tuple.class ).list();
			assertThat( list ).hasSize( 1 );

			final Tuple result = list.get( 0 );
			assertThat( result.get( 0 ) ).isEqualTo( 1 );
			assertThat( result.get( "id" ) ).isEqualTo( 1 );
			assertThat( result.get( 0 ) ).isSameAs( result.get( "id" ) );

			assertThat( result.get( 1 ) ).isInstanceOf( SimpleComposite.class );
			assertThat( result.get( 1 ) ).isSameAs( result.get( "composite" ) );
		});
	}

	@Test
	public void testBasicAndCompositeArray(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String qry = "select id, composite from SimpleEntity order by name";
			final List<Object[]> list = session.createQuery( qry ).list();
			assertThat( list ).hasSize( 1 );

			final Object[] result = list.get( 0 );
			assertThat( result[0] ).isEqualTo( 1 );
			assertThat( result[1] ).isInstanceOf( SimpleComposite.class );
		});
	}

	@Test
	public void testSelectingSamePathDifferentAliasOrder1(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String qry = "select id as id1, id as id2 from SimpleEntity order by id1";
			final List<Object[]> list = session.createQuery( qry ).list();
			assertThat( list ).hasSize( 1 );

			final Object[] result = list.get( 0 );
			assertThat( result[0] ).isEqualTo( 1 );
			assertThat( result[1] ).isEqualTo( 1 );
		});
	}

	@Test
	public void testSelectingSamePathDifferentAliasOrder2(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String qry = "select id as id1, id as id2 from SimpleEntity order by id2";
			final List<Object[]> list = session.createQuery( qry ).list();
			assertThat( list ).hasSize( 1 );

			final Object[] result = list.get( 0 );
			assertThat( result[0] ).isEqualTo( 1 );
			assertThat( result[1] ).isEqualTo( 1 );
		});
	}
}
