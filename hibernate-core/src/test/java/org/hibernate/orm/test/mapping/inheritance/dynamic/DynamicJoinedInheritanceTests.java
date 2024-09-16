/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.dynamic;

import java.util.HashMap;
import java.util.Map;

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
		scope.inTransaction( (session) -> {
			session.createQuery( "delete Sub" ).executeUpdate();
		} );
	}
}
