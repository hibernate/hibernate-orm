/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.customtype;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.customtype.EnumTypeEntity;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Assert;
import org.junit.Test;

import jakarta.persistence.EntityManager;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7780")
public class EnumTypeTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {EnumTypeEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( AvailableSettings.PREFER_NATIVE_ENUM_TYPES, "false" );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		em.getTransaction().begin();
		EnumTypeEntity entity = new EnumTypeEntity( EnumTypeEntity.E1.X, EnumTypeEntity.E2.A );
		em.persist( entity );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void testEnumRepresentation() {
		EntityManager entityManager = getEntityManager();
		entityManager.getTransaction().begin();

		final String qry = "SELECT enum1, enum2 FROM EnumTypeEntity_AUD ORDER BY REV ASC";
		Object[] results = (Object[]) entityManager.createNativeQuery( qry, "e1_e2" ).getSingleResult();
		entityManager.getTransaction().commit();
		entityManager.close();

		Assert.assertNotNull( results );
		Assert.assertEquals( 2, results.length );
		Assert.assertEquals( "X", results[0] );
		// Compare the Strings to account for, as an example, Oracle
		// returning a BigDecimal instead of an int.
		Assert.assertEquals( "0", results[1] + "" );
	}
}
