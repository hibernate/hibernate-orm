/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.produce;

import java.util.List;
import java.util.Map;

import org.hibernate.orm.test.query.sqm.BaseSqmUnitTest;
import org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem;
import org.hibernate.orm.test.query.sqm.produce.domain.InjectedLookupListItem;
import org.hibernate.orm.test.query.sqm.produce.domain.NestedCtorLookupListItem;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.select.SqmDynamicInstantiation;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.tree.expression.instantiation.DynamicInstantiationNature;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.TestingUtil;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.CollectionMatchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class DynamicInstantiationTests extends BaseSqmUnitTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityOfBasics.class,
		};
	}

	@Test
	public void testSimpleDynamicInstantiationSelection() {
		SqmSelectStatement statement = interpretSelect(
				"select new org.hibernate.orm.test.query.sqm.produce.domain.ConstructedLookupListItem( e.id, e.theString ) from EntityOfBasics e"
		);
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmDynamicInstantiation dynamicInstantiation = TestingUtil.cast(
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
			final SqmDynamicInstantiation instantiation = TestingUtil.cast(
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
			final SqmDynamicInstantiation instantiation = TestingUtil.cast(
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


		final SqmDynamicInstantiation instantiation = TestingUtil.cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);
		assertThat( instantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				is( equalTo( ConstructedLookupListItem.class ) )
		);
		assertThat( instantiation.getArguments(), hasSize( 2 ) );


		final SqmPath theIntegerPath = TestingUtil.cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 1 ).getSelectableNode(),
				SqmPath.class
		);
		assertThat( theIntegerPath.getReferencedNavigable().getNavigableName(), is( "theInteger" ) );
		assertThat( theIntegerPath.getReferencedNavigable().getJavaType(), is( equalTo( Integer.class ) ) );
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

		final SqmDynamicInstantiation<?> instantiation = TestingUtil.cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);
		assertThat( instantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
		assertThat(
				instantiation.getInstantiationTarget().getJavaType(),
				is( equalTo( NestedCtorLookupListItem.class ) )
		);
		assertThat( instantiation.getArguments(), hasSize( 3 ) );

		final SqmPath firstArg = TestingUtil.cast(
				instantiation.getArguments().get( 0 ).getSelectableNode(),
				SqmPath.class
		);
		assertThat( firstArg.getReferencedNavigable().getNavigableName(), is( "id" ) );

		final SqmPath secondArg = TestingUtil.cast(
				instantiation.getArguments().get( 1 ).getSelectableNode(),
				SqmPath.class
		);
		assertThat( secondArg.getReferencedNavigable().getNavigableName(), is( "theString" ) );


		final SqmDynamicInstantiation thirdArg = TestingUtil.cast(
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
		SqmSelectStatement<?> statement = interpretSelect( "select new list( e.id, e.theString ) from EntityOfBasics e" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmDynamicInstantiation<?> instantiation = TestingUtil.cast(
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
				instanceOf( SqmPath.class )
		);
		assertThat( instantiation.getArguments().get( 0 ).getAlias(), is( nullValue() ) );

		assertThat(
				instantiation.getArguments().get( 1 ).getSelectableNode(),
				instanceOf( SqmPath.class )
		);
		assertThat( instantiation.getArguments().get( 1 ).getAlias(), is( nullValue() ) );
	}

	@Test
	public void testSimpleDynamicMapInstantiation() {
		SqmSelectStatement<?> statement = interpretSelect( "select new map( e.id as id, e.theString as ts ) from EntityOfBasics e" );
		assertEquals( 1, statement.getQuerySpec().getSelectClause().getSelections().size() );

		final SqmDynamicInstantiation<?> instantiation = TestingUtil.cast(
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
				instanceOf( SqmPath.class )
		);
		assertThat( instantiation.getArguments().get( 0 ).getAlias(), is( "id" ) );

		assertThat(
				instantiation.getArguments().get( 1 ).getSelectableNode(),
				instanceOf( SqmPath.class )
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

		final SqmDynamicInstantiation dynamicInstantiation = TestingUtil.cast(
				statement.getQuerySpec().getSelectClause().getSelections().get( 0 ).getSelectableNode(),
				SqmDynamicInstantiation.class
		);

		assertThat( dynamicInstantiation.getInstantiationTarget().getNature(), is( DynamicInstantiationNature.CLASS ) );
		assertThat( dynamicInstantiation.getInstantiationTarget().getJavaType(), is( equalTo( InjectedLookupListItem.class ) ) );
		assertThat( dynamicInstantiation.getArguments(), hasSize( 2 ) );
	}
}
