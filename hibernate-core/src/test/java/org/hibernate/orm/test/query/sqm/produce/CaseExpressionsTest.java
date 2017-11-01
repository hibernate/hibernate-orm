/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.Person;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmLiteralInteger;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.expression.function.SqmCoalesceFunction;
import org.hibernate.query.sqm.tree.expression.function.SqmNullifFunction;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class CaseExpressionsTest extends BaseSqmUnitTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( Person.class );
	}

	@Test
	public void testBasicSimpleCaseExpression() {
		SqmSelectStatement select = interpretSelect(
				"select p from Person p where p.numberOfToes = case p.dob when ?1 then 6 else 8 end"
		);

		final RelationalSqmPredicate predicate = cast(
				select.getQuerySpec().getWhereClause().getPredicate(),
				RelationalSqmPredicate.class
		);

		final SqmCaseSimple caseStatement = cast(
				predicate.getRightHandExpression(),
				SqmCaseSimple.class
		);

		assertThat( caseStatement.getFixture(), notNullValue() );
		assertThat( caseStatement.getFixture(), instanceOf( SqmSingularAttributeReference.class ) );

		assertThat( caseStatement.getOtherwise(), notNullValue() );
		assertThat( caseStatement.getOtherwise(), instanceOf( SqmLiteralInteger.class ) );

		assertThat( caseStatement.getWhenFragments().size(), is(1) );
	}

	@Test
	public void testBasicSearchedCaseExpression() {
		SqmSelectStatement select = interpretSelect(
				"select p from Person p where p.numberOfToes = case when p.dob = ?1 then 6 else 8 end"
		);

		final RelationalSqmPredicate predicate = cast(
				select.getQuerySpec().getWhereClause().getPredicate(),
				RelationalSqmPredicate.class
		);

		final SqmCaseSearched caseStatement = cast(
				predicate.getRightHandExpression(),
				SqmCaseSearched.class
		);

		assertThat( caseStatement.getOtherwise(), notNullValue() );
		assertThat( caseStatement.getOtherwise(), instanceOf( SqmLiteralInteger.class ) );

		assertThat( caseStatement.getWhenFragments().size(), is(1) );
	}

	@Test
	public void testBasicCoalesceExpression() {
		SqmSelectStatement select = interpretSelect(
				"select coalesce(p.nickName, p.mate.nickName) from Person p"
		);

		assertThat( select.getQuerySpec().getSelectClause().getSelections(), hasSize( 1 ) );

		final SqmCoalesceFunction coalesce = cast(
				select.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmCoalesceFunction.class
		);

		assertThat( coalesce.getArguments(), hasSize( 2 ) );
		assertEquals( coalesce.getJavaTypeDescriptor().getJavaType(), String.class );
	}

	@Test
	public void testBasicNullifExpression() {
		SqmSelectStatement select = interpretSelect(
				"select nullif(p.nickName, p.mate.nickName) from Person p"
		);

		assertThat( select.getQuerySpec().getSelectClause().getSelections(), hasSize( 1 ) );
		final SqmNullifFunction nullif = cast(
				select.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmNullifFunction.class
		);

		assertEquals( nullif.getJavaTypeDescriptor().getJavaType(), String.class );
	}

}
