/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.domain.Person;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmCaseSearched;
import org.hibernate.query.sqm.tree.expression.SqmCaseSimple;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.expression.SqmHqlNumericLiteral;
import org.hibernate.query.sqm.tree.predicate.SqmComparisonPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;

import org.hibernate.testing.orm.junit.TestingUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class CaseExpressionsTest extends BaseSqmUnitTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	public void testBasicSimpleCaseExpression() {
		SqmSelectStatement<?> select = interpretSelect(
				"select p from Person p where p.numberOfToes = case p.dob when ?1 then 6 else 8 end"
		);

		final SqmComparisonPredicate predicate = TestingUtil.cast(
				select.getQuerySpec().getWhereClause().getPredicate(),
				SqmComparisonPredicate.class
		);

		final SqmCaseSimple caseStatement = TestingUtil.cast(
				predicate.getRightHandExpression(),
				SqmCaseSimple.class
		);

		assertThat( caseStatement.getFixture(), notNullValue() );
		assertThat( caseStatement.getFixture(), instanceOf( SqmPath.class ) );

		assertThat( caseStatement.getOtherwise(), notNullValue() );
		assertThat( caseStatement.getOtherwise(), instanceOf( SqmHqlNumericLiteral.class ) );

		assertThat( caseStatement.getWhenFragments().size(), is(1) );
	}

	@Test
	public void testBasicSearchedCaseExpression() {
		SqmSelectStatement<?> select = interpretSelect(
				"select p from Person p where p.numberOfToes = case when p.dob = ?1 then 6 else 8 end"
		);

		final SqmComparisonPredicate predicate = TestingUtil.cast(
				select.getQuerySpec().getWhereClause().getPredicate(),
				SqmComparisonPredicate.class
		);

		final SqmCaseSearched caseStatement = TestingUtil.cast(
				predicate.getRightHandExpression(),
				SqmCaseSearched.class
		);

		assertThat( caseStatement.getOtherwise(), notNullValue() );
		assertThat( caseStatement.getOtherwise(), instanceOf( SqmHqlNumericLiteral.class ) );

		assertThat( caseStatement.getWhenFragments().size(), is(1) );
	}

	@Test
	public void testBasicCoalesceExpression() {
		SqmSelectStatement<?> select = interpretSelect(
				"select coalesce(p.nickName, p.mate.nickName) from Person p"
		);

		assertThat( select.getQuerySpec().getSelectClause().getSelections(), hasSize( 1 ) );

		final SqmFunction<?> coalesce = TestingUtil.cast(
				select.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmFunction.class
		);

		assertThat( coalesce.getArguments(), hasSize( 2 ) );
		assertEquals( coalesce.getJavaTypeDescriptor().getJavaTypeClass(), String.class );
	}

	@Test
	public void testBasicNullifExpression() {
		SqmSelectStatement<?> select = interpretSelect(
				"select nullif(p.nickName, p.mate.nickName) from Person p"
		);

		assertThat( select.getQuerySpec().getSelectClause().getSelections(), hasSize( 1 ) );
		final SqmSelectableNode<?> selectableNode = select.getQuerySpec()
				.getSelectClause()
				.getSelections()
				.get( 0 )
				.getSelectableNode();

		assertEquals( selectableNode.getJavaTypeDescriptor().getJavaTypeClass(), String.class );

//		final  nullif = TestingUtil.cast(
//				selectableNode,
//				SqmNullifFunction.class
//		);

	}

}
