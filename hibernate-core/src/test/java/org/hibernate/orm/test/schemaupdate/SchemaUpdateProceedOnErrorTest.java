/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@Jpa(
		annotatedClasses = SchemaUpdateProceedOnErrorTest.From.class,
		integrationSettings = @Setting(name = HBM2DDL_AUTO, value = "update")
)
public class SchemaUpdateProceedOnErrorTest {

	@Test
	public void testHaltOnError(EntityManagerFactoryScope factoryScope) {
		try {
			factoryScope.getEntityManagerFactory();
		}
		catch ( Exception e ) {
			fail( "Should not halt on error: " + e.getMessage() );
		}
	}

	@Entity(name = "From")
	public static class From {

		@Id
		private Integer id;

		private String table;

		private String select;
	}
}
