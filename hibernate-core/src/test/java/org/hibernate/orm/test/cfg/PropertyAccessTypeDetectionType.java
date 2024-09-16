/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cfg;

import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@JiraKey(value ="HHH-12199")
public class PropertyAccessTypeDetectionType extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "cfg/FooEntity.hbm.xml" };
	}

	public static class FooEntity {
		public static final String intValue = "intValue";

		private Long id;
		private Integer _intValue;

		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }

		public Integer getIntValue() { return _intValue; }
		public void setIntValue(Integer intValue) { this._intValue = intValue; }
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPropertyAccessIgnoresStaticFields() {
		// verify that the entity persister is configured with property intValue as an Integer rather than
		// using the static field reference and determining the type to be String.
		assertTrue(
				sessionFactory().getRuntimeMetamodels()
						.getMappingMetamodel()
						.getEntityDescriptor( FooEntity.class )
						.getPropertyType( "intValue" )
						.getReturnedClass()
						.isAssignableFrom( Integer.class )
		);
	}
}
