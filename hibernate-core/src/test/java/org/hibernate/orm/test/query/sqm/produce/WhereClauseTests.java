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
import org.hibernate.orm.test.support.domains.gambit.EntityOfLists;
import org.hibernate.orm.test.support.domains.gambit.EntityOfMaps;
import org.hibernate.orm.test.support.domains.gambit.EntityOfSets;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.SqmCollectionSize;
import org.hibernate.query.sqm.tree.expression.SqmLiteralInteger;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.predicate.NullnessSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.RelationalPredicateOperator;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for elements of WHERE clauses.
 *
 * @author Gunnar Morling
 */
@SuppressWarnings("WeakerAccess")
public class WhereClauseTests extends BaseSqmUnitTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Person.class );
		metadataSources.addAnnotatedClass( EntityOfLists.class );
		metadataSources.addAnnotatedClass( EntityOfSets.class );
		metadataSources.addAnnotatedClass( EntityOfMaps.class );
	}

	@Test
	public void testIsNotNullPredicate() {
		SqmSelectStatement statement = interpretSelect( "select l from Person l where l.nickName is not null" );
		assertThat( statement.getQuerySpec().getWhereClause().getPredicate(), instanceOf( NullnessSqmPredicate.class ) );
		NullnessSqmPredicate predicate = (NullnessSqmPredicate) statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate.isNegated(), is(true) );
	}

	@Test
	public void testNotIsNullPredicate() {
		SqmSelectStatement statement = interpretSelect( "select l from Person l where not l.nickName is null" );
		assertThat( statement.getQuerySpec().getWhereClause().getPredicate(), instanceOf( NullnessSqmPredicate.class ) );
		NullnessSqmPredicate predicate = (NullnessSqmPredicate) statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate.isNegated(), is(true) );
	}

	@Test
	public void testNotIsNotNullPredicate() {
		SqmSelectStatement statement = interpretSelect( "select l from Person l where not l.nickName is not null" );
		assertThat( statement.getQuerySpec().getWhereClause().getPredicate(), instanceOf( NullnessSqmPredicate.class ) );
		NullnessSqmPredicate predicate = (NullnessSqmPredicate) statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate.isNegated(), is(false) );
	}

	@Test
	public void testCollectionSizeFunction() {
		SqmSelectStatement statement = interpretSelect( "SELECT t FROM EntityOfSets t WHERE SIZE( t.setOfBasics ) = 311" );

		SqmPredicate predicate = statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate, instanceOf( RelationalSqmPredicate.class ) );
		RelationalSqmPredicate relationalPredicate = ( (RelationalSqmPredicate) predicate );

		assertThat( relationalPredicate.getOperator(), is( RelationalPredicateOperator.EQUAL ) );

		assertThat( relationalPredicate.getRightHandExpression(), instanceOf( SqmLiteralInteger.class ) );
		assertThat( ( (SqmLiteralInteger) relationalPredicate.getRightHandExpression() ).getLiteralValue(), is( 311 ) );

		assertThat(
				relationalPredicate.getLeftHandExpression(),
				instanceOf( SqmCollectionSize.class )
		);
		final SqmCollectionSize func = (SqmCollectionSize) relationalPredicate.getLeftHandExpression();
		assertThat(
				func.getPluralAttributeBinding().getSourceReference().getExportedFromElement().getIdentificationVariable(),
				is( "t" )
		);
		assertThat(
				func.getPluralAttributeBinding().getReferencedNavigable().getAttributeName(),
				is( "setOfBasics" )
		);
	}

	@Test
	public void testListIndexFunction() {
		SqmSelectStatement statement = interpretSelect( "select l from EntityOfLists t join t.listOfBasics l where index(l) > 2" );

		SqmPredicate predicate = statement.getQuerySpec().getWhereClause().getPredicate();
		assertThat( predicate, instanceOf( RelationalSqmPredicate.class ) );
		RelationalSqmPredicate relationalPredicate = ( (RelationalSqmPredicate) predicate );

		assertThat( relationalPredicate.getOperator(), is( RelationalPredicateOperator.GREATER_THAN ) );

		assertThat( relationalPredicate.getRightHandExpression(), instanceOf( SqmLiteralInteger.class ) );
		assertThat( ( (SqmLiteralInteger) relationalPredicate.getRightHandExpression() ).getLiteralValue(), is( 2 ) );

		assertThat( relationalPredicate.getLeftHandExpression(), instanceOf( SqmCollectionIndexReference.class ) );
		final SqmPluralAttributeReference collectionBinding = ( (SqmCollectionIndexReference) relationalPredicate.getLeftHandExpression() ).getSourceReference();
		assertThat( collectionBinding.getExportedFromElement().getIdentificationVariable(), is( "l" ) );
	}
}
