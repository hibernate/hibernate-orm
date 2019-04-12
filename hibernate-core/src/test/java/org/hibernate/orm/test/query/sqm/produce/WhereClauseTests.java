/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.Person;
import org.hibernate.query.spi.ComparisonOperator;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmLiteral;
import org.hibernate.query.sqm.tree.predicate.SqmNullnessPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.domain.gambit.EntityOfMaps;
import org.hibernate.testing.orm.domain.gambit.EntityOfSets;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for elements of WHERE clauses.
 *
 * @author Gunnar Morling
 */
@SuppressWarnings("WeakerAccess")
public class WhereClauseTests extends BaseSqmUnitTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				EntityOfLists.class,
				EntityOfSets.class,
				EntityOfMaps.class,
		};
	}

	@Test
	public void testIsNotNullPredicate() {
		SqmSelectStatement statement = interpretSelect( "select l from Person l where l.nickName is not null" );
		assertThat( statement.getQuerySpec().getWhereClause().getPredicate(), instanceOf( SqmNullnessPredicate.class ) );
		SqmNullnessPredicate predicate = (SqmNullnessPredicate) statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate.isNegated(), is(true) );
	}

	@Test
	public void testNotIsNullPredicate() {
		SqmSelectStatement statement = interpretSelect( "select l from Person l where not l.nickName is null" );
		assertThat( statement.getQuerySpec().getWhereClause().getPredicate(), instanceOf( SqmNullnessPredicate.class ) );
		SqmNullnessPredicate predicate = (SqmNullnessPredicate) statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate.isNegated(), is(true) );
	}

	@Test
	public void testNotIsNotNullPredicate() {
		SqmSelectStatement statement = interpretSelect( "select l from Person l where not l.nickName is not null" );
		assertThat( statement.getQuerySpec().getWhereClause().getPredicate(), instanceOf( SqmNullnessPredicate.class ) );
		SqmNullnessPredicate predicate = (SqmNullnessPredicate) statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate.isNegated(), is(false) );
	}

	@Test
	public void testCollectionSizeFunction() {
		SqmSelectStatement statement = interpretSelect( "SELECT t FROM EntityOfSets t WHERE SIZE( t.setOfBasics ) = 311" );

		SqmPredicate predicate = statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate, instanceOf( SqmComparisonPredicate.class ) );
		SqmComparisonPredicate relationalPredicate = ( (SqmComparisonPredicate) predicate );

		assertThat( relationalPredicate.getSqmOperator(), is( ComparisonOperator.EQUAL ) );

		assertThat( relationalPredicate.getRightHandExpression(), instanceOf( SqmLiteral.class ) );
		assertThat( ( (SqmLiteral) relationalPredicate.getRightHandExpression() ).getLiteralValue(), is( 311 ) );

		assertThat( relationalPredicate.getLeftHandExpression(), instanceOf( SqmCollectionSize.class ) );

		final SqmCollectionSize func = (SqmCollectionSize) relationalPredicate.getLeftHandExpression();
		assertThat( func.getPluralPath().getLhs().getExplicitAlias(), is( "t" ) );
		assertThat( func.getPluralPath().getReferencedNavigable().getNavigableName(), is( "setOfBasics" ) );
	}

	@Test
	public void testListIndexFunction() {
		SqmSelectStatement statement = interpretSelect( "select l from EntityOfLists t join t.listOfBasics l where index(l) > 2" );

		SqmPredicate predicate = statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate, instanceOf( SqmComparisonPredicate.class ) );
		SqmComparisonPredicate relationalPredicate = ( (SqmComparisonPredicate) predicate );

		assertThat( relationalPredicate.getSqmOperator(), is( ComparisonOperator.GREATER_THAN ) );

		assertThat( relationalPredicate.getRightHandExpression(), instanceOf( SqmLiteral.class ) );
		assertThat( ( (SqmLiteral) relationalPredicate.getRightHandExpression() ).getLiteralValue(), is( 2 ) );

		assertThat( relationalPredicate.getLeftHandExpression(), instanceOf( SqmPath.class ) );
		final SqmPath indexPath = (SqmPath) relationalPredicate.getLeftHandExpression();

		assertThat( indexPath.getLhs(), notNullValue() );
		assertThat( indexPath.getLhs().getExplicitAlias(), is( "l" ) );
	}
}
