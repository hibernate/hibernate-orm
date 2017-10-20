/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import java.util.List;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.boot.MetadataSources;
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

import org.junit.Test;

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
public class AliasCollisionTest extends BaseSqmUnitTest {

	@Test(expected = AliasCollisionException.class)
	public void testDuplicateResultVariableCollision() {
		// in both cases the query is using an alias (`b`) as 2 different
		// select-clause result variables - that's an error
		interpretSelect( "select a.address as b, a.name as b from Anything a" );

		interpretSelect( "select a.address as b, a.address as b from Anything a" );
	}

	@Test(expected = AliasCollisionException.class)
	public void testResultVariableRenamesIdentificationVariableCollision() {
		interpretSelect( "select a.basic as a from Anything as a" );

		// NOTE that there is a special form of this rule.  consider:
		//
		//		select {alias} as {alias} from XYZ as {alias}
		//
		// this is valid because its just the explicit form of what happens
		// when you have:
		//
		//		select {alias} from XYZ as {alias}
	}

	@Test(expected = AliasCollisionException.class)
	public void testDuplicateIdentificationVariableCollision() {
		interpretSelect( "select a from Anything as a, SomethingElse as a" );
		interpretSelect(
				"select a.address as b, a.basic as c from Anything a where a.basic2 in " +
				"(select b.basic3 as e from SomethingElse as b, Something as b)"
		);
		interpretSelect(
				"select a from Something a left outer join a.entity a on a.basic1 > 5"
		);

	}

	@Test
	public void testSameIdentificationVariablesInSubquery() {
		final String query = "select a from Anything a where a.basic1 in ( select a from SomethingElse a where a.basic = 5)";
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
		final String query = "select a from Anything a where a.basic in " +
				"( select b.basic from SomethingElse b where a.basic = b.basic2 )";
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

		metadataSources.addAnnotatedClass( Entity.class );
		metadataSources.addAnnotatedClass( Anything.class );
		metadataSources.addAnnotatedClass( Something.class );
		metadataSources.addAnnotatedClass( SomethingElse.class );
	}

	@javax.persistence.Entity( name = "Entity" )
	public static class Entity {
		@Id
		public Integer id;

		String basic;
		String basic1;
	}

	@javax.persistence.Entity( name = "Anything" )
	public static class Anything {
		@Id
		public Integer id;

		String address;
		String name;
		Long basic;
		Long basic1;
		Long basic2;
		Long basic3;
		Long b;
	}

	@javax.persistence.Entity( name = "Something" )
	public static class Something {
		@Id
		public Integer id;

		Long basic;
		Long basic1;
		Long basic2;
		Long basic3;
		Long basic4;

		@ManyToOne
		Entity entity;
	}

	@javax.persistence.Entity( name = "SomethingElse" )
	public static class SomethingElse {
		@Id
		public Integer id;

		Long basic;
		Long basic1;
		Long basic2;
		Long basic3;

		@ManyToOne
		Entity entity;
	}
}
