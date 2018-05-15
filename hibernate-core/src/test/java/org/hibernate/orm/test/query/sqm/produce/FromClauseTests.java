/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.Person;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.from.SqmFromClause;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;
import org.hibernate.query.sqm.tree.select.SqmSelection;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;
import static org.hibernate.testing.hamcrest.sqm.SqmAliasMatchers.isImplicitAlias;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Initial work on a "from clause processor"
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class FromClauseTests extends BaseSqmUnitTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( Person.class );
	}

	@Test
	public void testSimpleFrom() {
		final SqmSelectStatement selectStatement = interpretSelect( "select p.nickName from Person p" );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );
		assertThat( firstSpace.getJoins(), isEmpty() );

		final SqmRoot firstSpaceRoot = firstSpace.getRoot();
		assertThat( firstSpaceRoot, notNullValue() );
		assertThat( firstSpaceRoot.getIdentificationVariable(), is( "p") );
	}

	@Test
	public void testMultipleSpaces() {
		final SqmSelectStatement selectStatement = interpretSelect(
				"select p.nickName from Person p, Person p2"
		);

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();

		assertNotNull( fromClause );
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 2 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );
		assertThat( firstSpace.getJoins(), isEmpty() );
		assertThat( firstSpace.getRoot(), notNullValue() );
		assertThat( firstSpace.getRoot().getIdentificationVariable(), is( "p")  );

		final SqmFromElementSpace secondSpace = fromClause.getFromElementSpaces().get( 1 );
		assertThat( secondSpace, notNullValue() );
		assertThat( secondSpace.getJoins(), isEmpty() );
		assertThat( secondSpace.getRoot(), notNullValue() );
		assertThat( secondSpace.getRoot().getIdentificationVariable(), is( "p2")  );
	}

	@Test
	public void testImplicitAlias() {
		final SqmSelectStatement selectStatement = interpretSelect( "select nickName from Person" );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );
		assertThat( firstSpace.getJoins(), isEmpty() );
		assertThat( firstSpace.getRoot(), notNullValue() );
		assertTrue( ImplicitAliasGenerator.isImplicitAlias( firstSpace.getRoot().getIdentificationVariable() ) );
	}

	@Test
	public void testCrossJoin() {
		final SqmSelectStatement selectStatement = interpretSelect(
				"select p.nickName from Person p cross join Person p2"
		);

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );
		assertThat( firstSpace.getJoins(), hasSize( 1 ) );
		assertThat( firstSpace.getRoot(), notNullValue() );

		assertThat( firstSpace.getRoot().getIdentificationVariable(), is( "p" )  );
		assertThat( firstSpace.getJoins().get( 0 ).getIdentificationVariable(), is( "p2" )  );
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
			SqmSelectStatement selectStatement,
			SqmJoinType joinType,
			String rootAlias,
			String joinAlias) {
		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );

		assertThat( firstSpace.getRoot(), notNullValue() );
		assertThat( firstSpace.getRoot().getIdentificationVariable(), is( rootAlias )  );

		assertThat( firstSpace.getJoins(), hasSize( 1 ) );
		assertThat( firstSpace.getJoins().get( 0 ).getIdentificationVariable(), is( joinAlias )  );
		assertThat( firstSpace.getJoins().get( 0 ).getJoinType(), is( joinType ) );
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
		final SqmSelectStatement selectStatement = interpretSelect(
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
		SqmSelectStatement selectStatement = interpretSelect( query );


		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );

		assertThat( firstSpace.getRoot(), notNullValue() );
		assertThat( firstSpace.getRoot().getIdentificationVariable(), is( "p" )  );

		assertThat( firstSpace.getJoins(), hasSize( 1 ) );
		assertThat( firstSpace.getJoins().get( 0 ).getIdentificationVariable(), isImplicitAlias() );
	}

	@Test
	public void testFromElementReferenceInSelect() {
		final String query = "select p from Person p";
		SqmSelectStatement selectStatement = interpretSelect( query );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final SqmFromElementSpace firstSpace = fromClause.getFromElementSpaces().get( 0 );
		assertThat( firstSpace, notNullValue() );

		assertThat( selectStatement.getQuerySpec().getSelectClause().getSelections(), hasSize( 1 ) );
		final SqmSelection sqmSelection = selectStatement.getQuerySpec().getSelectClause().getSelections().get( 0 );

		assertThat( sqmSelection.getSelectableNode(), instanceOf( SqmEntityReference.class ) );
	}

	@Test
	public void testFromElementReferenceInOrderBy() {
		final String query = "select p from Person p order by p";
		SqmSelectStatement selectStatement = interpretSelect( query );

		final SqmFromClause fromClause = selectStatement.getQuerySpec().getFromClause();
		assertThat( fromClause, notNullValue() );
		assertThat( fromClause.getFromElementSpaces(), hasSize( 1 ) );

		final List<SqmSortSpecification> orderBy = selectStatement.getQuerySpec()
				.getOrderByClause()
				.getSortSpecifications();
		assertThat( orderBy, hasSize( 1 ) );

		assertThat( orderBy.get( 0 ).getSortExpression(), instanceOf( SqmEntityReference.class ) );
	}

	@Test
	public void testCrossSpaceReferencesFail() {
		final String query = "select p from Person p, Person p2 join Person p3 on p3.id = p.id";
		try {
			interpretSelect( query );
			fail( "Expecting failure" );
		}
		catch (SemanticException e) {
			assertThat( e.getMessage(), startsWith( "Qualified join predicate referred to FromElement [" ) );
			assertThat( e.getMessage(), endsWith( "] outside the FromElementSpace containing the join" ) );
		}
	}

}
