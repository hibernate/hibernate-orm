/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.List;

import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.domain.Person;
import org.hibernate.query.SemanticException;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Initial work on a "from clause processor"
 *
 * @author Steve Ebersole
 */
public class FromClauseTests extends BaseSqmUnitTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	public void testSimpleFrom() {
		final SqmSelectStatement<?> selectStatement = interpretSelect( "select p.nickName from Person p" );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final SqmRoot<?> firstRoot = fromClause.getRoots().get( 0 );
		assertThat( firstRoot, notNullValue() );
		assertThat( firstRoot.getJoins(), isEmpty() );
		assertThat( firstRoot.getExplicitAlias(), is( "p") );
	}

	@Test
	public void testMultipleSpaces() {
		final SqmSelectStatement<?> selectStatement = interpretSelect(
				"select p.nickName from Person p, Person p2"
		);

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();

		assertNotNull( fromClause );
		assertThat( fromClause, notNullValue() );

		assertThat( fromClause.getRoots(), hasSize( 2 ) );

		final SqmRoot<?> firstRoot = fromClause.getRoots().get( 0 );
		assertThat( firstRoot, notNullValue() );
		assertThat( firstRoot.getJoins(), isEmpty() );
		assertThat( firstRoot.getExplicitAlias(), is( "p") );

		final SqmRoot<?> secondRoot = fromClause.getRoots().get( 0 );
		assertThat( secondRoot, notNullValue() );
		assertThat( secondRoot.getJoins(), isEmpty() );
		assertThat( secondRoot.getExplicitAlias(), is( "p") );
	}

	@Test
	public void testImplicitAlias() {
		final SqmSelectStatement<?> selectStatement = interpretSelect( "select nickName from Person" );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final SqmRoot<?> sqmRoot = fromClause.getRoots().get( 0 );
		assertThat( sqmRoot, notNullValue() );
		assertThat( sqmRoot.getJoins(), isEmpty() );
		assertThat( sqmRoot.getExplicitAlias(), nullValue() );
	}

	@Test
	public void testCrossJoin() {
		final SqmSelectStatement<?> selectStatement = interpretSelect(
				"select p.nickName from Person p cross join Person p2"
		);

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final SqmRoot<?> sqmRoot = fromClause.getRoots().get( 0 );
		assertThat( sqmRoot, notNullValue() );
		assertThat( sqmRoot.getExplicitAlias(), is( "p" )  );
		assertThat( sqmRoot.getSqmJoins(), hasSize( 1 ) );
		assertThat( sqmRoot.getSqmJoins().get( 0 ).getExplicitAlias(), is( "p2" )  );
	}

	@Test
	public void testSimpleImplicitInnerJoin() {
		simpleJoinAssertions(
				interpretSelect( "select p.nickName from Person p join p.mate m" ),
				SqmJoinType.INNER,
				"p",
				"m"
		);
	}

	private void simpleJoinAssertions(
			SqmSelectStatement<?> selectStatement,
			SqmJoinType joinType,
			String rootAlias,
			String joinAlias) {
		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final SqmRoot<?> sqmRoot = fromClause.getRoots().get( 0 );
		assertThat( sqmRoot, notNullValue() );
		assertThat( sqmRoot.getExplicitAlias(), is( rootAlias )  );

		assertThat( sqmRoot.getJoins(), hasSize( 1 ) );
		assertThat( sqmRoot.getSqmJoins().get( 0 ).getExplicitAlias(), is( joinAlias )  );
		assertThat( sqmRoot.getSqmJoins().get( 0 ).getSqmJoinType(), is( joinType ) );
	}

	@Test
	public void testSimpleExplicitInnerJoin() {
		simpleJoinAssertions(
				interpretSelect( "select a.nickName from Person a inner join a.mate c" ),
				SqmJoinType.INNER,
				"a",
				"c"
		);
	}

	@Test
	public void testSimpleExplicitOuterJoin() {
		simpleJoinAssertions(
				interpretSelect( "select a.nickName from Person a outer join a.mate c" ),
				SqmJoinType.LEFT,
				"a",
				"c"
		);
	}

	@Test
	public void testSimpleExplicitLeftOuterJoin() {
		simpleJoinAssertions(
				interpretSelect( "select a.nickName from Person a left outer join a.mate c" ),
				SqmJoinType.LEFT,
				"a",
				"c"
		);
	}

	@Test
	public void testAttributeJoinWithOnClause() {
		final SqmSelectStatement<?> selectStatement = interpretSelect(
				"select a from Person a left outer join a.mate c on c.numberOfToes > 5 and c.numberOfToes < 20 "
		);

		simpleJoinAssertions(
				selectStatement,
				SqmJoinType.LEFT,
				"a",
				"c"
		);

		// todo (6.0) : check join restrictions
		//		not yet tracked, nor exposed.  SqmPredicate

//		final SqmJoin join = selectStatement.getQuerySpec()
//				.getFromClause()
//				.getFromElementSpaces()
//				.get( 0 )
//				.getJoins()
//				.get( 0 );
	}

	@Test
	public void testPathExpression() {
		final String query = "select p.mate from Person p";
		SqmSelectStatement<?> selectStatement = interpretSelect( query );


		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final SqmRoot<?> sqmRoot = fromClause.getRoots().get( 0 );
		assertThat( sqmRoot, notNullValue() );
		assertThat( sqmRoot.getExplicitAlias(), is( "p" )  );
		assertThat( sqmRoot.getSqmJoins(), hasSize( 0 ) );
	}

	@Test
	public void testFromElementReferenceInSelect() {
		final String query = "select p from Person p";
		SqmSelectStatement<?> selectStatement = interpretSelect( query );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final SqmRoot<?> sqmRoot = fromClause.getRoots().get( 0 );
		assertThat( sqmRoot, notNullValue() );

		assertThat( selectStatement.getQuerySpec().getSelectClause().getSelections(), hasSize( 1 ) );
		final SqmSelection<?> sqmSelection = selectStatement.getQuerySpec().getSelectClause().getSelections().get( 0 );

		assertThat( sqmSelection.getSelectableNode(), instanceOf( SqmRoot.class ) );
	}

	@Test
	public void testFromElementReferenceInOrderBy() {
		final String query = "select p from Person p order by p";
		SqmSelectStatement<?> selectStatement = interpretSelect( query );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getRoots(), hasSize( 1 ) );

		final List<SqmSortSpecification> orderBy = selectStatement.getQuerySpec()
				.getOrderByClause()
				.getSortSpecifications();
		assertThat( orderBy, hasSize( 1 ) );

		assertThat( orderBy.get( 0 ).getSortExpression(), instanceOf( SqmRoot.class ) );
	}

	@Test
	public void testCrossSpaceReferencesFail() {
		final String query = "select p from Person p, Person p2 join Person p3 on p3.id = p.id";
		try {
			interpretSelect( query );
			fail( "Expecting failure" );
		}
		catch (SemanticException e) {
			assertThat( e.getMessage(), startsWith( "SqmQualifiedJoin predicate referred to SqmRoot [" ) );
			assertThat( e.getMessage(), containsString( "] other than the join's root [" ) );
		}
	}

}
