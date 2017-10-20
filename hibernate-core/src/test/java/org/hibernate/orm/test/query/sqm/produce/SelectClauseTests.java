/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import java.util.List;
import java.util.Map;

import org.hibernate.boot.MetadataSources;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.metamodel.model.domain.spi.CollectionElement.ElementClassification;
import org.hibernate.metamodel.model.domain.spi.CollectionIndex.IndexClassification;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.Person;
import org.hibernate.orm.test.support.domains.gambit.EntityOfLists;
import org.hibernate.orm.test.support.domains.gambit.EntityOfMaps;
import org.hibernate.query.sqm.tree.SqmQuerySpec;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.SqmBinaryArithmetic;
import org.hibernate.query.sqm.tree.expression.domain.AbstractSqmCollectionIndexReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmCollectionElementReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmMapEntryBinding;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.from.SqmFromElementSpace;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Test various forms of selections
 *
 * @author Steve Ebersole
 */
@Ignore( "Boot model building has problem binding some of this model" )
public class SelectClauseTests extends BaseSqmUnitTest {
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testSimpleAliasSelection() {
		SqmSelectStatement statement = interpretSelect( "select p from Person p" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		SqmSelection selection = statement.getQuerySpec().getSelectClause().getSelections().get( 0 );
		assertThat( selection.getSelectableNode(), instanceOf( SqmEntityReference.class ) );
	}

	@Test
	public void testSimpleAttributeSelection() {
		SqmSelectStatement statement = interpretSelect( "select p.nickName from Person p" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		SqmSelection selection = statement.getQuerySpec().getSelectClause().getSelections().get( 0 );
		assertThat( selection.getSelectableNode(), instanceOf( SqmSingularAttributeReference.class ) );
	}

	@Test
	public void testCompoundAttributeSelection() {
		SqmSelectStatement statement = interpretSelect( "select p.nickName, p.name.first from Person p" );
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
	}

	@Test
	public void testMixedAliasAndAttributeSelection() {
		SqmSelectStatement statement = interpretSelect( "select p, p.nickName from Person p" );
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmEntityReference.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
	}

	@Test
	public void testSimpleDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.sqm.test.hql.SelectClauseTests$DTO(p.nickName, p.numberOfToes) from Person p"
		);
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmDynamicInstantiation.class )
		);
	}

	@Test
	public void testMultipleDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.sqm.test.hql.SelectClauseTests$DTO(p.nickName, p.numberOfToes), " +
						"new org.hibernate.sqm.test.hql.SelectClauseTests$DTO(p.nickName, p.numberOfToes) " +
						"from Person p"
		);
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
				instanceOf( SqmDynamicInstantiation.class )
		);
	}

	@Test
	public void testMixedAttributeAndDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.sqm.test.hql.SelectClauseTests$DTO(p.nickName, p.numberOfToes), p.nickName from Person p"
		);
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
	}

	@Test
	public void testNestedDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.sqm.test.hql.SelectClauseTests$DTO(" +
						"    p.nickName, " +
						"    p.numberOfToes, " +
						"    new org.hibernate.sqm.test.hql.SelectClauseTests$DTO(p.nickName, p.numberOfToes) " +
						" ) " +
						"from Person p"
		);
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmDynamicInstantiation.class )
		);

		SqmDynamicInstantiation dynamicInstantiation = (SqmDynamicInstantiation) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode();
		assertThat(
				dynamicInstantiation.getInstantiationTarget().getNature(),
				equalTo( DynamicInstantiationNature.CLASS )
		);
		assertThat(
				dynamicInstantiation.getInstantiationTarget().getJavaType(),
				equalTo( DTO.class )
		);

		assertEquals( 3, dynamicInstantiation.getArguments().size() );
		assertThat(
				dynamicInstantiation.getArguments().get( 0 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
		assertThat(
				dynamicInstantiation.getArguments().get( 1 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
		assertThat(
				dynamicInstantiation.getArguments().get( 2 ).getSelectableNode(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		SqmDynamicInstantiation nestedInstantiation = (SqmDynamicInstantiation) dynamicInstantiation.getArguments().get( 2 ).getSelectableNode();
		assertThat(
				nestedInstantiation.getInstantiationTarget().getNature(),
				equalTo( DynamicInstantiationNature.CLASS )
		);
		assertThat(
				nestedInstantiation.getInstantiationTarget().getJavaType(),
				equalTo( DTO.class )
		);

	}

	@Test
	public void testSimpleDynamicListInstantiation() {
		SqmSelectStatement statement = interpretSelect( "select new list(p.nickName, p.numberOfToes) from Person p" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		SqmDynamicInstantiation instantiation = (SqmDynamicInstantiation) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode();
		assertThat(
				instantiation.getInstantiationTarget().getNature(),
				equalTo( DynamicInstantiationNature.LIST )
		);
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				equalTo( List.class )
		);

		assertEquals( 2, instantiation.getArguments().size() );
		assertThat(
				instantiation.getArguments().get( 0 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
		assertThat(
				instantiation.getArguments().get( 1 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
	}

	@Test
	public void testSimpleDynamicMapInstantiation() {
		SqmSelectStatement statement = interpretSelect( "select new map(p.nickName as nn, p.numberOfToes as nt) from Person p" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmDynamicInstantiation.class )
		);
		SqmDynamicInstantiation instantiation = (SqmDynamicInstantiation) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode();
		assertThat(
				instantiation.getInstantiationTarget().getNature(),
				equalTo( DynamicInstantiationNature.MAP )
		);
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				equalTo( Map.class )
		);

		assertEquals( 2, instantiation.getArguments().size() );
		assertThat(
				instantiation.getArguments().get( 0 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
		assertThat(
				instantiation.getArguments().get( 1 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
	}

	@Test
	public void testBinaryArithmeticExpression() {
		final String query = "select p.numberOfToes + p.numberOfToes as b from Person p";
		final SqmSelectStatement selectStatement = interpretSelect( query );

		final SqmQuerySpec querySpec = selectStatement.getQuerySpec();
		final SqmSelection selection = querySpec.getSelectClause().getSelections().get( 0 );

		assertThat( querySpec.getFromClause().getFromElementSpaces().size(), is(1) );
		final SqmFromElementSpace fromElementSpace = querySpec.getFromClause().getFromElementSpaces().get( 0 );
		final SqmRoot root = fromElementSpace.getRoot();
		assertThat( root.getNavigableReference().getReferencedNavigable().getEntityName(), endsWith( "Person" ) );
		assertThat( fromElementSpace.getJoins().size(), is(0) );

		SqmBinaryArithmetic expression = (SqmBinaryArithmetic) selection.getSelectableNode();
		SqmSingularAttributeReference leftHandOperand = (SqmSingularAttributeReference) expression.getLeftHandOperand();
		assertThat( leftHandOperand.getSourceReference().getExportedFromElement(), sameInstance( root ) );
		assertThat( leftHandOperand.getReferencedNavigable().getAttributeName(), is( "numberOfToes" ) );
//		assertThat( leftHandOperand.getFromElement(), nullValue() );

		SqmSingularAttributeReference rightHandOperand = (SqmSingularAttributeReference) expression.getRightHandOperand();
		assertThat( rightHandOperand.getSourceReference().getExportedFromElement(), sameInstance( root ) );
		assertThat( rightHandOperand.getReferencedNavigable().getAttributeName(), is( "numberOfToes" ) );
//		assertThat( leftHandOperand.getFromElement(), nullValue() );
	}

	@Test
	public void testBinaryArithmeticExpressionWithMultipleFromSpaces() {
		final String query = "select p.numberOfToes + p2.numberOfToes as b from Person p, Person p2";
		final SqmSelectStatement selectStatement = interpretSelect( query );

		final SqmQuerySpec querySpec = selectStatement.getQuerySpec();
		final SqmSelection selection = querySpec.getSelectClause().getSelections().get( 0 );

		assertThat( querySpec.getFromClause().getFromElementSpaces().size(), is(2) );

		final SqmRoot entityRoot = querySpec.getFromClause().getFromElementSpaces().get( 0 ).getRoot();
		assertThat( entityRoot.getNavigableReference().getReferencedNavigable().getEntityName(), endsWith( "Person" ) );

		final SqmRoot entity2Root = querySpec.getFromClause().getFromElementSpaces().get( 1 ).getRoot();
		assertThat( entity2Root.getNavigableReference().getReferencedNavigable().getEntityName(), endsWith( "Person" ) );

		SqmBinaryArithmetic addExpression = (SqmBinaryArithmetic) selection.getSelectableNode();

		SqmSingularAttributeReference leftHandOperand = (SqmSingularAttributeReference) addExpression.getLeftHandOperand();
		assertThat( leftHandOperand.getSourceReference().getExportedFromElement(), sameInstance( entityRoot ) );
		assertThat( leftHandOperand.getReferencedNavigable().getAttributeName(), is( "numberOfToes" ) );

		SqmSingularAttributeReference rightHandOperand = (SqmSingularAttributeReference) addExpression.getRightHandOperand();
		assertThat( rightHandOperand.getSourceReference().getExportedFromElement(), sameInstance( entity2Root ) );
		assertThat( rightHandOperand.getReferencedNavigable().getAttributeName(), is( "numberOfToes" ) );
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
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( AbstractSqmCollectionIndexReference.class )
		);

		final AbstractSqmCollectionIndexReference mapKeyPathExpression = (AbstractSqmCollectionIndexReference) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode();
		final PluralPersistentAttribute attribute = mapKeyPathExpression.getPluralAttributeBinding().getReferencedNavigable();
		assertThat( attribute.getPersistentCollectionDescriptor().getCollectionClassification(), is( collectionClassification ) );
		assertThat( attribute.getPersistentCollectionDescriptor().getIndexDescriptor().getClassification(), is( indexClassification) );
		assertThat( mapKeyPathExpression.getExpressableType(), sameInstance( attribute ) );
	}

	@Test
	public void testMapValueFunction() {
		collectionValueFunctionAssertions(
				interpretSelect( "select value(m) from EntityOfMaps e join e.basicToBasicMap m" ),
				CollectionClassification.MAP,
				ElementClassification.BASIC,
				"m"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(m) from EntityOfMaps e join e.basicToComponentMap m" ),
				CollectionClassification.MAP,
				ElementClassification.EMBEDDABLE,
				"m"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(m) from EntityOfMaps e join e.basicToOneToMany m" ),
				CollectionClassification.MAP,
				ElementClassification.ONE_TO_MANY,
				"m"
		);
	}

	@Test
	public void testCollectionValueFunction() {
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfLists e join e.listOfBasics b" ),
				CollectionClassification.LIST,
				ElementClassification.BASIC,
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfLists e join e.listOfComponents b" ),
				CollectionClassification.LIST,
				ElementClassification.EMBEDDABLE,
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfLists e join e.listOfOneToMany b" ),
				CollectionClassification.LIST,
				ElementClassification.ONE_TO_MANY,
				"b"
		);
		// todo : ManyToMany not properly handled atm

		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfSets e join e.setOfBasics b" ),
				CollectionClassification.SET,
				ElementClassification.BASIC,
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfSets e join e.setOfComponents b" ),
				CollectionClassification.SET,
				ElementClassification.EMBEDDABLE,
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfSets e join e.setOfOneToMany b" ),
				CollectionClassification.SET,
				ElementClassification.ONE_TO_MANY,
				"b"
		);
		// todo : ManyToMany not properly handled atm

		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfMaps e join e.basicToBasicMap b" ),
				CollectionClassification.MAP,
				ElementClassification.BASIC,
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfMaps e join e.basicToComponentMap b" ),
				CollectionClassification.MAP,
				ElementClassification.EMBEDDABLE,
				"b"
		);
		collectionValueFunctionAssertions(
				interpretSelect( "select value(b) from EntityOfMaps e join e.basicToOneToMany b" ),
				CollectionClassification.MAP,
				ElementClassification.ONE_TO_MANY,
				"b"
		);
		// todo : ManyToMany not properly handled atm
	}

	private void collectionValueFunctionAssertions(
			SqmSelectStatement statement,
			CollectionClassification collectionClassification,
			ElementClassification elementClassification,
			String collectionIdentificationVariable) {
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmCollectionElementReference.class )
		);

		final SqmCollectionElementReference elementBinding = (SqmCollectionElementReference) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode();
		final SqmPluralAttributeReference attrRef = elementBinding.getSourceReference();

		assertThat( attrRef.getReferencedNavigable().getPersistentCollectionDescriptor().getCollectionClassification(), is( collectionClassification) );
//		assertThat( elementBinding.getSelectableNodeType(), sameInstance( attrRef.getElementDescriptor().getType() ) );
		assertThat( attrRef.getReferencedNavigable().getPersistentCollectionDescriptor().getElementDescriptor().getClassification(), is( elementClassification ) );
		assertThat( attrRef.getExportedFromElement().getIdentificationVariable(), is( collectionIdentificationVariable ) );
	}

	@Test
	public void testMapEntryFunction() {
		SqmSelectStatement statement = interpretSelect( "select entry(m) from EntityOfMaps e join e.basicToManyToMany m" );

		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );
		assertThat(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				instanceOf( SqmMapEntryBinding.class )
		);

		final SqmMapEntryBinding mapEntryFunction = (SqmMapEntryBinding) statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode();
		assertThat( mapEntryFunction.getAttributeAttributeReference().getExportedFromElement(), notNullValue() );
		assertThat( mapEntryFunction.getAttributeAttributeReference().getExportedFromElement().getIdentificationVariable(), is( "m") );

		final PluralPersistentAttribute attribute = mapEntryFunction.getAttributeAttributeReference().getReferencedNavigable();
		assertThat( attribute.getPersistentCollectionDescriptor().getCollectionClassification(), is( CollectionClassification.MAP) );

		// Key
		assertThat( attribute.getPersistentCollectionDescriptor().getIndexDescriptor().getClassification(), is( IndexClassification.BASIC) );
		assertEquals( String.class, attribute.getPersistentCollectionDescriptor().getIndexDescriptor().getJavaTypeDescriptor().getJavaType() );

		// value/element
		assertThat( attribute.getPersistentCollectionDescriptor().getElementDescriptor().getClassification(), is( ElementClassification.ONE_TO_MANY) );
		assertThat( ( (SqmEntityReference) attribute.getPersistentCollectionDescriptor().getElementDescriptor() ).getExpressableType().getEntityName(), is( "org.hibernate.sqm.test.domain.EntityOfMaps" ) );
		assertEquals( EntityOfMaps.class, attribute.getPersistentCollectionDescriptor().getElementDescriptor().getJavaTypeDescriptor().getJavaType() );
	}

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );

		metadataSources.addAnnotatedClass( Person.class );
		metadataSources.addAnnotatedClass( EntityOfLists.class );
	}

	public static class DTO {
	}
}
