/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entityname;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author stliu
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/entityname/Vehicle.hbm.xml")
@SessionFactory
public class EntityNameFromSubClassTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testEntityName(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
			Person stliu = new Person();
			stliu.setName("stliu");
			Car golf = new Car();
			golf.setOwner("stliu");
			stliu.getCars().add(golf);
			s.persist(stliu);
		} );

		factoryScope.inTransaction( (s) -> {
			Person p = s.find( Person.class, 1 );
			Assertions.assertEquals( 1, p.getCars().size() );
			Assertions.assertEquals( Car.class, p.getCars().iterator().next().getClass() );
		} );
	}

}
