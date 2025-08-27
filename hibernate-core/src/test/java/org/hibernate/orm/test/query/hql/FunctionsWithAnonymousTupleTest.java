/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = EntityOfBasics.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-16425" )
public class FunctionsWithAnonymousTupleTest {
	@BeforeAll
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityOfBasics eob1 = new EntityOfBasics( 1 );
			eob1.setTheInt( 1 );
			eob1.setTheString( "the_string" );
			session.persist( eob1 );
			final EntityOfBasics eob2 = new EntityOfBasics( 2 );
			eob2.setTheInt( 2 );
			eob2.setTheString( "the_string" );
			session.persist( eob2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from EntityOfBasics" ).executeUpdate() );
	}

	@Test
	public void testCount(SessionFactoryScope scope) {
		final List<Tuple> resultList = executeQuery( scope, "count(*)", true );
		assertThat( resultList ).hasSize( 1 );
		assertThat( resultList.get( 0 ).get( 1 ) ).isEqualTo( 2L );
	}

	@Test
	public void testMin(SessionFactoryScope scope) {
		final List<Tuple> resultList = executeQuery( scope, "min(a.theInt)", true );
		assertThat( resultList ).hasSize( 1 );
		assertThat( resultList.get( 0 ).get( 1 ) ).isEqualTo( 1 );
	}

	@Test
	public void testMax(SessionFactoryScope scope) {
		final List<Tuple> resultList = executeQuery( scope, "max(a.theInt)", true );
		assertThat( resultList ).hasSize( 1 );
		assertThat( resultList.get( 0 ).get( 1 ) ).isEqualTo( 2 );
	}

	@Test
	public void testSum(SessionFactoryScope scope) {
		final List<Tuple> resultList = executeQuery( scope, "sum(a.theInt)", true );
		assertThat( resultList ).hasSize( 1 );
		assertThat( resultList.get( 0 ).get( 1 ) ).isEqualTo( 3L );
	}

	@Test
	public void testAverage(SessionFactoryScope scope) {
		final List<Tuple> resultList = executeQuery( scope, "avg(a.theInt)", true );
		assertThat( resultList ).hasSize( 1 );
		assertThat( resultList.get( 0 ).get( 1 ) ).isEqualTo( 1.5d );
	}

	@Test
	public void testFloor(SessionFactoryScope scope) {
		final List<Tuple> resultList = executeQuery( scope, "floor(a.theInt)", false );
		assertThat( resultList ).hasSize( 2 );
		assertThat( resultList.get( 0 ).get( 1 ) ).isEqualTo( 1 );
		assertThat( resultList.get( 1 ).get( 1 ) ).isEqualTo( 2 );
	}

	private List<Tuple> executeQuery(SessionFactoryScope scope, String aggregateExpression, boolean aggregate) {
		return scope.fromTransaction( session -> session.createQuery( String.format(
				"select a.theString, %s from " +
				"(select theString as theString, theInt as theInt from EntityOfBasics) as a " +
				( aggregate ? "group by a.theString" : "" ),
				aggregateExpression
		), Tuple.class ).getResultList() );
	}
}
