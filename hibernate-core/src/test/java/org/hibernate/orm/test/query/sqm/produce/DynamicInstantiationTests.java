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
import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem;
import org.hibernate.orm.test.query.sqm.produce.domain.InjectedLookupListItem;
import org.hibernate.orm.test.query.sqm.produce.domain.NestedCtorLookupListItem;
import org.hibernate.orm.test.support.domains.gambit.EntityOfBasics;
import org.hibernate.query.sqm.tree.SqmSelectStatement;
import org.hibernate.query.sqm.tree.expression.domain.SqmEntityIdentifierReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmSingularAttributeReference;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.sql.ast.tree.spi.expression.instantiation.DynamicInstantiationNature;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class DynamicInstantiationTests extends BaseSqmUnitTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( EntityOfBasics.class );
	}

	@Test
	public void testSimpleDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ) from EntityOfBasics e"
		);
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmDynamicInstantiation dynamicInstantiation = cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);

		assertThat( dynamicInstantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
		assertThat( dynamicInstantiation.getInstantiationTarget().getJavaType(), is( equalTo( ConstructedLookupListItem.class ) ) );
		assertThat( dynamicInstantiation.getArguments(), hasSize( 2 ) );
	}

	@Test
	public void testMultipleDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ), " +
						"new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ) " +
						"from EntityOfBasics e"
		);
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );


		{
			final SqmDynamicInstantiation instantiation = cast(
					statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
					SqmDynamicInstantiation.class
			);
			assertThat( instantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
			assertThat(
					instantiation.getInstantiationTarget().getJavaType(),
					is( equalTo( ConstructedLookupListItem.class ) )
			);
			assertThat( instantiation.getArguments(), hasSize( 2 ) );
		}

		{
			final SqmDynamicInstantiation instantiation = cast(
					statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
					SqmDynamicInstantiation.class
			);
			assertThat( instantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
			assertThat(
					instantiation.getInstantiationTarget().getJavaType(),
					is( equalTo( ConstructedLookupListItem.class ) )
			);
			assertThat( instantiation.getArguments(), hasSize( 2 ) );
		}
	}

	@Test
	public void testMixedAttributeAndDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ), e.theInteger from EntityOfBasics e"
		);
		assertEquals( 2, statement.getQuerySpec().getSelectClause().getSelections().size() );


		final SqmDynamicInstantiation instantiation = cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);
		assertThat( instantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				is( equalTo( ConstructedLookupListItem.class ) )
		);
		assertThat( instantiation.getArguments(), hasSize( 2 ) );


		final SqmSingularAttributeReference attrRef = cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
				SqmSingularAttributeReference.class
		);
		assertThat( attrRef.getReferencedNavigable().getAttributeName(), is( "theInteger" ) );
		assertThat( attrRef.getReferencedNavigable().getJavaType(), is( equalTo( Integer.class ) ) );
	}

	@Test
	public void testNestedDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.orm.test.query.sqm.produce.domain.NestedCtorLookupListItem(" +
						" e.id, " +
						" e.theString, " +
						" new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString )" +
						" ) " +
						" from EntityOfBasics e"
		);
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmDynamicInstantiation instantiation = cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);
		assertThat( instantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				is( equalTo( NestedCtorLookupListItem.class ) )
		);
		assertThat( instantiation.getArguments(), hasSize( 3 ) );

		final SqmEntityIdentifierReference firstArg = cast(
				instantiation.getArguments().get( 0 ).getSelectableNode(),
				SqmEntityIdentifierReference.class
		);
		assertThat( firstArg.getReferencedNavigable().getNavigableName(), is( "id" ) );

		final SqmSingularAttributeReference secondArg = cast(
				instantiation.getArguments().get( 1 ).getSelectableNode(),
				SqmSingularAttributeReference.class
		);
		assertThat( secondArg.getReferencedNavigable().getNavigableName(), is( "theString" ) );


		final SqmDynamicInstantiation thirdArg = cast(
				instantiation.getArguments().get( 2 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);
		assertThat( thirdArg.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
		assertThat(
				thirdArg.getInstantiationTarget().getJavaType(),
				is( equalTo( ConstructedLookupListItem.class ) )
		);
		assertThat( thirdArg.getArguments(), hasSize( 2 ) );
	}

	@Test
	public void testSimpleDynamicListInstantiation() {
		SqmSelectStatement statement = interpretSelect( "select new list( e.id, e.theString ) from EntityOfBasics e" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmDynamicInstantiation instantiation = cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);
		assertThat(
				instantiation.getInstantiationTarget().getNature(),
				equalTo( DynamicInstantiationNature.LIST )
		);
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				is( equalTo( List.class ) )
		);

		assertThat( instantiation.getArguments(), hasSize( 2 ) );

		assertThat(
				instantiation.getArguments().get( 0 ).getSelectableNode(),
				instanceOf( SqmEntityIdentifierReference.class )
		);
		assertThat( instantiation.getArguments().get( 0 ).getAlias(), is( nullValue() ) );

		assertThat(
				instantiation.getArguments().get( 1 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
		assertThat( instantiation.getArguments().get( 1 ).getAlias(), is( nullValue() ) );
	}

	@Test
	public void testSimpleDynamicMapInstantiation() {
		SqmSelectStatement statement = interpretSelect( "select new map( e.id as id, e.theString as ts ) from EntityOfBasics e" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmDynamicInstantiation instantiation = cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);

		assertThat(
				instantiation.getInstantiationTarget().getNature(),
				equalTo( DynamicInstantiationNature.MAP )
		);
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				is( equalTo( Map.class ) )
		);

		assertEquals( 2, instantiation.getArguments().size() );

		assertThat(
				instantiation.getArguments().get( 0 ).getSelectableNode(),
				instanceOf( SqmEntityIdentifierReference.class )
		);
		assertThat( instantiation.getArguments().get( 0 ).getAlias(), is( "id" ) );

		assertThat(
				instantiation.getArguments().get( 1 ).getSelectableNode(),
				instanceOf( SqmSingularAttributeReference.class )
		);
		assertThat( instantiation.getArguments().get( 1 ).getAlias(), is( "ts" ) );
	}

	@Test
	public void testSimpleInjectedInstantiation() {
		// todo (6.0) : this should blow up as early as possible - no aliases for bean-injection-based dynamic-instantiation
		//		atm this does not fail until later when building the SQL AST

		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.orm.test.query.sqm.produce.domain.InjectedLookupListItem( e.id, e.theString ) from EntityOfBasics e"
		);
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmDynamicInstantiation dynamicInstantiation = cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);

		assertThat( dynamicInstantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
		assertThat( dynamicInstantiation.getInstantiationTarget().getJavaType(), is( equalTo( InjectedLookupListItem.class ) ) );
		assertThat( dynamicInstantiation.getArguments(), hasSize( 2 ) );
	}
}
