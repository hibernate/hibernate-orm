/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import java.util.Map;

import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.metamodel.model.domain.spi.CollectionElement;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex.IndexClassification;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralValuedNavigable;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.Person;
import org.hibernate.query.sqm.tree.domain.SqmMapEntryReference;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.EntityOfLists;
import org.hibernate.testing.orm.domain.gambit.EntityOfMaps;
import org.hibernate.testing.orm.domain.gambit.EntityOfSets;
import org.hibernate.testing.orm.junit.TestingUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test various forms of selections
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class SelectClauseTests extends BaseSqmUnitTest {

	@Test
	public void testSimpleAliasSelection() {
		SqmSelectStatement statement = interpretSelect( "select p from Person p" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		SqmSelection selection = statement.getQuerySpec().getSelectClause().getSelections().get( 0 );
		assertThat( selection.getSelectableNode(), instanceOf( SqmRoot.class ) );
	}

	@Test
	public void testSimpleAttributeSelection() {
		SqmSelectStatement statement = interpretSelect( "select p.nickName from Person p" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		SqmSelection selection = statement.getQuerySpec().getSelectClause().getSelections().get( 0 );
		assertThat( selection.getSelectableNode(), instanceOf( SqmNavigableReference.class ) );
	}

	@Test
	public void testCompoundAttributeSelection() {
		SqmSelectStatement statement = interpretSelect( "select p.nickName, p.name.first from Person p" );
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmNavigableReference.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
				instanceOf( SqmNavigableReference.class )
		);
	}

	@Test
	public void testMixedAliasAndAttributeSelection() {
		SqmSelectStatement statement = interpretSelect( "select p, p.nickName from Person p" );
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmRoot.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
				instanceOf( SqmNavigableReference.class )
		);
	}

	@Test
	public void testBinaryArithmeticExpression() {
		final String query = "select p.numberOfToes + p.numberOfToes as b from Person p";
		final SqmSelectStatement selectStatement = interpretSelect( query );

		final SqmQuerySpec querySpec = selectStatement.getQuerySpec();
		final SqmSelection selection = querySpec.getSelectClause().getSelections().get( 0 );

		assertThat( querySpec.getFromClause().getRoots().size(), is(1) );
		final SqmRoot root = querySpec.getFromClause().getRoots().get( 0 );
		assertThat( root.getReferencedNavigable().getEntityName(), endsWith( "Person" ) );
		assertThat( root.getJoins().size(), is(0) );

		SqmBinaryArithmetic expression = (SqmBinaryArithmetic) selection.getSelectableNode();
		SqmPath leftHandOperand = (SqmPath) expression.getLeftHandOperand();
		assertThat( leftHandOperand.getLhs(), sameInstance( root ) );
		assertThat( leftHandOperand.getReferencedNavigable().getNavigableName(), is( "numberOfToes" ) );
//		assertThat( leftHandOperand.getFromElement(), nullValue() );

		SqmPath rightHandOperand = (SqmPath) expression.getRightHandOperand();
		assertThat( rightHandOperand.getLhs(), sameInstance( root ) );
		assertThat( rightHandOperand.getReferencedNavigable().getNavigableName(), is( "numberOfToes" ) );
//		assertThat( leftHandOperand.getFromElement(), nullValue() );
	}

	@Test
	public void testBinaryArithmeticExpressionWithMultipleFromSpaces() {
		final String query = "select p.numberOfToes + p2.numberOfToes as b from Person p, Person p2";
		final SqmSelectStatement selectStatement = interpretSelect( query );

		final SqmQuerySpec querySpec = selectStatement.getQuerySpec();
		final SqmSelection selection = querySpec.getSelectClause().getSelections().get( 0 );

		assertThat( querySpec.getFromClause().getRoots().size(), is(2) );

		final SqmRoot entityRoot = querySpec.getFromClause().getRoots().get( 0 );
		assertThat( entityRoot.getReferencedNavigable().getEntityName(), endsWith( "Person" ) );

		final SqmRoot entity2Root = querySpec.getFromClause().getRoots().get( 1 );
		assertThat( entity2Root.getReferencedNavigable().getEntityName(), endsWith( "Person" ) );

		SqmBinaryArithmetic addExpression = (SqmBinaryArithmetic) selection.getSelectableNode();

		SqmPath leftHandOperand = (SqmPath) addExpression.getLeftHandOperand();
		assertThat( leftHandOperand.getLhs(), sameInstance( entityRoot ) );
		assertThat( leftHandOperand.getReferencedNavigable().getNavigableName(), is( "numberOfToes" ) );

		SqmPath rightHandOperand = (SqmPath) addExpression.getRightHandOperand();
		assertThat( rightHandOperand.getLhs(), sameInstance( entity2Root ) );
		assertThat( rightHandOperand.getReferencedNavigable().getNavigableName(), is( "numberOfToes" ) );
	}

	@Test
	public void testMapKeyFunction() {
		collectionIndexFunctionAssertions(
				interpretSelect( "select key(m) from EntityOfMaps e join e.basicToBasicMap m" ),
				CollectionClassification.MAP,
				IndexClassification.BASIC,
				"m"
		);
		collectionIndexFunctionAssertions(
				interpretSelect( "select key(m) from EntityOfMaps e join e.componentToBasicMap m" ),
				CollectionClassification.MAP,
				IndexClassification.EMBEDDABLE,
				"m"
		);
	}

	private void collectionIndexFunctionAssertions(
			SqmSelectStatement statement,
			CollectionClassification collectionClassification,
			IndexClassification indexClassification,
			String collectionAlias) {
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmSelectableNode selectedExpr = statement.getQuerySpec()
				.getSelectClause()
				.getSelections()
				.get( 0 )
				.getSelectableNode();

		assertThat( selectedExpr, instanceOf( SqmNavigableReference.class ) );
		final SqmNavigableReference selectedPath = (SqmNavigableReference) selectedExpr;

		final PersistentCollectionDescriptor collectionDescriptor = selectedPath.getLhs()
				.as( PluralValuedNavigable.class )
				.getCollectionDescriptor();

		assertThat( collectionDescriptor.getCollectionClassification(), is( collectionClassification ) );
		assertThat( collectionDescriptor.getIndexDescriptor().getClassification(), is( indexClassification) );
		assertThat( selectedPath.getExpressableType(), sameInstance( collectionDescriptor.getIndexDescriptor() ) );
	}

	@Test
	public void testMapValueFunction() {
		collectionValueFunctionAssertions(
				interpretSelect( "select value(m) from EntityOfMaps e join e.basicToBasicMap m" ),
				"m"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(m) from EntityOfMaps e join e.basicToComponentMap m" ),
				"m"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(m) from EntityOfMaps e join e.basicToOneToMany m" ),
				"m"
		);
	}

	@Test
	public void testCollectionValueFunction() {
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfLists e join e.listOfBasics b" ),
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfLists e join e.listOfComponents b" ),
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfLists e join e.listOfOneToMany b" ),
				"b"
		);

		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfSets e join e.setOfBasics b" ),
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfSets e join e.setOfComponents b" ),
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfSets e join e.setOfOneToMany b" ),
				"b"
		);
		// todo : ManyToMany not properly handled atm

		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfMaps e join e.basicToBasicMap b" ),
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfMaps e join e.basicToComponentMap b" ),
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfMaps e join e.basicToOneToMany b" ),
				"b"
		);
		// todo : ManyToMany not properly handled atm
	}

	private void collectionValueFunctionAssertions(
			SqmSelectStatement statement,
			String collectionIdentificationVariable) {
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmSelectableNode selectedExpression = statement.getQuerySpec()
				.getSelectClause()
				.getSelections()
				.get( 0 )
				.getSelectableNode();

		assertThat( selectedExpression, instanceOf( SqmPath.class ) );
		final SqmPath selectedPath = (SqmPath) selectedExpression;

		assertThat( selectedPath.getReferencedNavigable(), instanceOf( CollectionElement.class ) );
		assertThat( selectedPath.getLhs().getReferencedNavigable(), instanceOf( PluralValuedNavigable.class ) );

		assertThat( selectedPath.getLhs().getExplicitAlias(), is( collectionIdentificationVariable ) );
	}

	@Test
	public void testMapEntryFunction() {
		SqmSelectStatement statement = interpretSelect( "select entry(m) from EntityOfMaps e join e.basicToManyToMany m" );

		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmMapEntryReference mapEntryPath = (SqmMapEntryReference) statement.getQuerySpec()
				.getSelectClause()
				.getSelections()
				.get( 0 )
				.getSelectableNode();

		assertThat( mapEntryPath.getJavaTypeDescriptor().getJavaType(), is( equalTo( Map.Entry.class ) ) );

		final SqmPath selectedPathLhs = mapEntryPath.getMapPath();
		assertThat( selectedPathLhs.getExplicitAlias(), is( "m" ) );
	}

	@Test
	public void testSimpleRootEntitySelection() {
		SqmSelectStatement statement = interpretSelect( "select e from EntityOfBasics e" );

		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		final SqmPath sqmEntityReference = TestingUtil.cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmPath.class
		);

		assertThat( sqmEntityReference.getJavaTypeDescriptor().getJavaType(), equalTo( EntityOfBasics.class ));
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class,
				EntityOfBasics.class,
				EntityOfLists.class,
				EntityOfMaps.class,
				EntityOfSets.class,
		};
	}

}
