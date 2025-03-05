/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitymode.map.subclass;

import java.util.HashMap;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class SubclassDynamicMapTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "entitymode/map/subclass/Mappings.hbm.xml" };
	}

	@Test
	public void testConcreateSubclassDeterminationOnEmptyDynamicMap() {
		Session s = openSession();
		s.beginTransaction();
		s.persist( "Superclass", new HashMap() );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Superclass" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
