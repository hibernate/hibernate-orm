/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.support.domains.gambit.EntityWithManyToOneSelfReference;
import org.hibernate.orm.test.support.domains.gambit.SimpleEntity;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.query.sqm.AliasCollisionException;
import org.hibernate.query.sqm.produce.spi.ImplicitAliasGenerator;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.predicate.InSubQuerySqmPredicate;
import org.hibernate.query.sqm.tree.predicate.RelationalSqmPredicate;
import org.hibernate.query.sqm.tree.select.SqmSelection;

import org.hibernate.testing.junit5.ExpectedException;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.hibernate.testing.hamcrest.CollectionMatchers.isEmpty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test of all alias collision scenarios
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@SuppressWarnings("WeakerAccess")
public class AliasCollisionTest extends BaseSqmUnitTest {

	@Test
	@ExpectedException( AliasCollisionException.class )
	public void testDuplicateResultVariableCollision() {
		// in both cases the query is using an alias (`b`) as 2 different
		// select-clause result variables - that's an error
		interpretSelect( "select a.someString as b, a.someInteger as b from SimpleEntity a" );
		interpretSelect( "select a.someInteger as b, a.someInteger as b from SimpleEntity a" );
	}

	@Test
	@ExpectedException( AliasCollisionException.class )
	public void testResultVariableRenamesIdentificationVariableCollision() {
		interpretSelect( "select a.someString as a from SimpleEntity as a" );

		// NOTE that there is a special form of this rule.  consider:
		//
		//		select {alias} as {alias} from XYZ as {alias}
		//
		// this is valid because its just the explicit form of what happens
		// when you have:
		//
		//		select {alias} from XYZ as {alias}
	}

	@Test
	@ExpectedException( AliasCollisionException.class )
	public void testDuplicateIdentificationVariableCollision() {
		interpretSelect( "select a from SimpleEntity as a, SimpleEntity as a" );
		interpretSelect(
				"select a.someString as b, a.someInteger as c from SimpleEntity a where a.someLong in " +
				"(select b.someLong as e from SimpleEntity as b, SimpleEntity as b)"
		);
		interpretSelect(
				"select a from EntityWithManyToOneSelfReference a left outer join a.other a on a.someInteger > 5"
		);

	}

	@Test
	public void testSameIdentificationVariablesInSubquery() {
		final String query = "select a from SimpleEntity a where a.someString in (select a.someString from SimpleEntity a where a.someInteger = 5)";
		final SqmSelectStatement sqm = interpretSelect( query );

		final SqmQuerySpec querySpec = sqm.getQuerySpec();

		final List<SqmSelection> selections = querySpec.getSelectClause().getSelections();
		assertThat( selections, hasSize( 1 ) );
		assertTrue( ImplicitAliasGenerator.isImplicitAlias( selections.get( 0 ).getAlias() ) );

		final List<SqmFromElementSpace> spaces = querySpec.getFromClause().getFromElementSpaces();
		assertThat( spaces, hasSize( 1 ) );
		assertThat( spaces.get( 0 ).getJoins(), isEmpty() );
		assertThat( spaces.get( 0 ).getRoot().getIdentificationVariable(), is( "a" ) );

		assertThat( querySpec.getWhereClause().getPredicate(), instanceOf( InSubQuerySqmPredicate.class ) );
		final InSubQuerySqmPredicate predicate = (InSubQuerySqmPredicate) querySpec.getWhereClause().getPredicate();

		final SqmFromElementSpace subQuerySpace = predicate.getSubQueryExpression()
				.getQuerySpec()
				.getFromClause()
				.getFromElementSpaces()
				.get( 0 );

		assertThat( subQuerySpace.getRoot().getIdentificationVariable(), is( "a" ) );
	}

	@Test
	public void testSubqueryUsingIdentificationVariableDefinedInRootQuery() {
		final String query = "select a from SimpleEntity a where a.someString in " +
				"( select b.someString from SimpleEntity b where a.someLong = b.someLong )";
		final SqmSelectStatement sqm = interpretSelect( query );

		final SqmQuerySpec querySpec = sqm.getQuerySpec();

		final List<SqmSelection> selections = querySpec.getSelectClause().getSelections();
		assertThat( selections, hasSize( 1 ) );
		assertTrue( ImplicitAliasGenerator.isImplicitAlias( selections.get( 0 ).getAlias() ) );

		final List<SqmFromElementSpace> spaces = querySpec.getFromClause().getFromElementSpaces();
		assertThat( spaces, hasSize( 1 ) );
		assertThat( spaces.get( 0 ).getJoins(), isEmpty() );
		assertThat( spaces.get( 0 ).getRoot().getIdentificationVariable(), is( "a" ) );

		assertThat( querySpec.getWhereClause().getPredicate(), instanceOf( InSubQuerySqmPredicate.class ) );
		final InSubQuerySqmPredicate predicate = (InSubQuerySqmPredicate) querySpec.getWhereClause().getPredicate();

		final SqmQuerySpec subQuerySpec = predicate.getSubQueryExpression().getQuerySpec();

		assertThat(
				subQuerySpec.getFromClause().getFromElementSpaces().get( 0 ).getRoot().getIdentificationVariable(),
				is( "b" )
		);

		final RelationalSqmPredicate correlation = (RelationalSqmPredicate) subQuerySpec.getWhereClause().getPredicate();
		final SqmNavigableReference leftHandExpression = (SqmNavigableReference) correlation.getLeftHandExpression();
		assertThat(
				leftHandExpression.getSourceReference().getIdentificationVariable(),
				is( "a" )
		);
		final SqmNavigableReference rightHandExpression = (SqmNavigableReference) correlation.getRightHandExpression();
		assertThat(
				rightHandExpression.getSourceReference().getIdentificationVariable(),
				is( "b" )
		);
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( EntityWithManyToOneSelfReference.class );
		metadataSources.addAnnotatedClass( SimpleEntity.class );
		metadataSources.addAnnotatedClass( EntityWithManyToOneSelfReference.class );
	}
}
