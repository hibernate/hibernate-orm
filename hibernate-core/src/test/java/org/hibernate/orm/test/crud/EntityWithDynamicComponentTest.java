/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.crud;

import org.hibernate.boot.MetadataSources;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;
import org.hibernate.orm.test.support.domains.gambit.EntityOfDynamicComponent;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Chris Cranford
 */
public class EntityWithDynamicComponentTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addResource( "org/hibernate/test/dynamic-components/EntityOfDynamicComponent.hbm.xml" );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}

	@Test
	public void testDynamicComponentLifecycle() {
		// Create entity
		sessionFactoryScope().inTransaction( session -> {
			final EntityOfDynamicComponent entity = new EntityOfDynamicComponent();
			entity.setId( 1L );
			entity.setNote( "Initial Commit" );
			entity.getValues().put( "v1", 25 );
			entity.getValuesWithProperties().put( "prop1", 50 );
			entity.getValuesWithProperties().put( "prop2", "Initial String" );
			session.save( entity );
		} );

		// Test entity was saved properly
		sessionFactoryScope().inTransaction( session -> {
			final EntityOfDynamicComponent entity = session.find( EntityOfDynamicComponent.class, 1L );
			assertThat( entity, notNullValue() );
			assertThat( entity.getNote(), is( "Initial Commit" ) );
			// this is only size = 1 because of $type$
			assertThat( entity.getValues().size(), is( 1 ) );
			// this is only size = 3 because of inclusion of $type$
			assertThat( entity.getValuesWithProperties().size(), is( 3 ) );
			assertThat( entity.getValuesWithProperties().get( "prop1" ), is( 50 ) );
			assertThat( entity.getValuesWithProperties().get( "prop2" ), is( "Initial String" ) );
		} );

		// todo (6.0) - it seems isDirty is seeing the state of the HashMaps not changing
		// 		so right now updates won't work in terms of validating that the values change.

//		// Update entity
//		sessionFactoryScope().inTransaction( session -> {
//			final EntityOfDynamicComponent entity = session.find( EntityOfDynamicComponent.class, 1 );
//			entity.setNote( "Updated Note" );
//			entity.getValues().put( "v2", 30 );
//			entity.getValuesWithProperties().remove( "prop1" );
//			entity.getValuesWithProperties().put( "prop1", 75 );
//			session.update( entity );
//		} );
//
//		// Test entity was updated properly
//		sessionFactoryScope().inTransaction( session -> {
//			final EntityOfDynamicComponent entity = session.find( EntityOfDynamicComponent.class, 1 );
//			assertThat( entity, notNullValue() );
//			assertThat( entity.getNote(), is( "Updated Note" ) );
//			// this is only size = 1 because of $type$
//			assertThat( entity.getValues().size(), is( 1 ) );
//			// this is only size = 3 because of inclusion of $type$
//			assertThat( entity.getValuesWithProperties().size(), is( 3 ) );
//			assertThat( entity.getValuesWithProperties().get( "prop1" ), is( 75 ) );
//			assertThat( entity.getValuesWithProperties().get( "prop2" ), is( "Initial String" ) );
//		} );

		// Delete entity
		sessionFactoryScope().inTransaction( session -> {
			session.delete( session.find( EntityOfDynamicComponent.class, 1L ) );
		} );

		// Test entity was deleted properly
		sessionFactoryScope().inTransaction( session -> {
			final EntityOfDynamicComponent entity = session.find( EntityOfDynamicComponent.class, 1L );
			assertThat( entity, nullValue() );
		} );
	}
}
