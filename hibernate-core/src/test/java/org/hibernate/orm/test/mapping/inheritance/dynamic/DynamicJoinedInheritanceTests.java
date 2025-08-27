/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.dynamic;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.graph.RootGraph;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.metamodel.internal.AbstractDynamicMapInstantiator.TYPE_KEY;

/**
 * @author Steve Ebersole
 */
@DomainModel( xmlMappings = "org/hibernate/orm/test/mapping/inheritance/dynamic/JoinedMappings.hbm.xml" )
@SessionFactory
public class DynamicJoinedInheritanceTests {
	@Test
	public void testLoading(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Map<String,?> entity = (Map<String, ?>) session.get( "Sub", 1 );
			assertThat( entity ).isNotNull();
			assertThat( entity.get( TYPE_KEY ) ).isNotNull();
			assertThat( entity.get( TYPE_KEY ) ).isEqualTo( "Sub" );
		} );
	}

	@Test
	public void testLoadingNewApi(SessionFactoryScope scope) {
		final RootGraph<Map<String, ?>> Sub_ =
				scope.getSessionFactory()
						.createGraphForDynamicEntity( "Sub" );
		scope.inTransaction( (session) -> {
			final Map<String,?> entity = session.find( Sub_, 1 );
			assertThat( entity ).isNotNull();
			assertThat( entity.get( "name" ) ).isEqualTo( "sub" );
			assertThat( entity.get( TYPE_KEY ) ).isNotNull();
			assertThat( entity.get( TYPE_KEY ) ).isEqualTo( "Sub" );
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final HashMap<Object, Object> entity = new HashMap<>();
			entity.put( "id", 1 );
			entity.put( "name", "sub" );
			entity.put( "subText", "" );
			session.persist( "Sub", entity );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
