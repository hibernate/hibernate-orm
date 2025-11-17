/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.orm.test.jpa.mapping.ColumnWithExplicitReferenceToPrimaryTableTest.AnEntity;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Hbm2ddlCreateOnlyTest {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	@JiraKey(value = "HHH-12955")
	public void testColumnAnnotationWithExplicitReferenceToPrimaryTable() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return List.of( AnEntity.class.getName() );
			}
		};


		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-only" );
		ServiceRegistryUtil.applySettings( settings );

		try (var emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build()) {
			emf.createEntityManager();
		}
	}
}
