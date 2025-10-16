/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.customtype;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.envers.entities.customtype.EnumTypeEntity;

import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.BeforeClassTemplate;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@JiraKey(value = "HHH-7780")
@EnversTest
@Jpa(
		annotatedClasses = {EnumTypeEntity.class},
		integrationSettings = @Setting(name = AvailableSettings.PREFER_NATIVE_ENUM_TYPES, value = "false")
)
public class EnumTypeTest {

	@BeforeClassTemplate
	public void initData(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			EnumTypeEntity entity = new EnumTypeEntity( EnumTypeEntity.E1.X, EnumTypeEntity.E2.A );
			em.persist( entity );
		} );
	}

	@Test
	public void testEnumRepresentation(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final String qry = "SELECT enum1, enum2 FROM EnumTypeEntity_AUD ORDER BY REV ASC";
			Object[] results = (Object[]) entityManager.createNativeQuery( qry, "e1_e2" ).getSingleResult();

			assertNotNull( results );
			assertEquals( 2, results.length );
			assertEquals( "X", results[0] );
			// Compare the Strings to account for, as an example, Oracle
			// returning a BigDecimal instead of an int.
			assertEquals( "0", results[1] + "" );
		} );
	}
}
