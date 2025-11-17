/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.entities.converter;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class BasicModelingTest {
	@Test
	@JiraKey( value = "HHH-9042" )
	public void testMetamodelBuilding() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Person.class )
					.getMetadataBuilder()
					.applyAttributeConverter( SexConverter.class )
					.build();

			( (MetadataImplementor) metadata ).orderColumns( false );
			( (MetadataImplementor) metadata ).validate();

			PersistentClass personBinding = metadata.getEntityBinding( Person.class.getName() );
			assertNotNull( personBinding );

			PersistentClass personAuditBinding = metadata.getEntityBinding( Person.class.getName() + "_AUD" );
			assertNotNull( personAuditBinding );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
