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
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/entitymode/map/subclass/Mappings.hbm.xml"
)
@SessionFactory
public class SubclassDynamicMapTest {

	@Test
	public void testConcreateSubclassDeterminationOnEmptyDynamicMap(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.persist( "Superclass", new HashMap<>() )
		);

		scope.inTransaction(
				session ->
						session.createMutationQuery( "delete Superclass" ).executeUpdate()
		);
	}
}
