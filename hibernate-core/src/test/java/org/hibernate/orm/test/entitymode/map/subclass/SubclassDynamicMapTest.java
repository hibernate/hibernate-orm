/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitymode.map.subclass;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/entitymode/map/subclass/Mappings.hbm.xml")
@SessionFactory
public class SubclassDynamicMapTest {
	@Test
	public void testConcreteSubclassDeterminationOnEmptyDynamicMap(SessionFactoryScope scope) {
		scope.inTransaction( (s) -> {
			s.persist( "Superclass", new HashMap() );
		} );

		scope.inTransaction( (s) -> {
			s.createMutationQuery( "delete Superclass" ).executeUpdate();
		} );
	}
}
