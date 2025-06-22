/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.generator.Generator;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests a UUID attribute annotated as a generated id value.
 *
 * @author Steve Ebersole
 */
@BaseUnitTest
@SkipForDialect( dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "Skipped for Sybase to avoid problems with UUIDs potentially ending with a trailing 0 byte")
public class GeneratedValueTest {
	@Test
	public void testGeneratedUuidId() throws Exception {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr ).addAnnotatedClass( TheEntity.class ).buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			PersistentClass entityBinding = metadata.getEntityBinding( TheEntity.class.getName() );
			assertEquals( UUID.class, entityBinding.getIdentifier().getType().getReturnedClass() );
			KeyValue keyValue = entityBinding.getIdentifier();
			Dialect dialect = metadata.getDatabase().getDialect();
			final Generator generator = keyValue.createGenerator( dialect, (RootClass) entityBinding);
			assertTyping( UuidGenerator.class, generator );

			// now a functional test
			SessionFactory sf = metadata.buildSessionFactory();
			try {
				TheEntity theEntity = new TheEntity();

				Session s = sf.openSession();
				s.beginTransaction();
				try {
					s.persist( theEntity );
					s.getTransaction().commit();
					s.close();

					assertNotNull( theEntity.id );

					s = sf.openSession();
					s.beginTransaction();

					s.remove( theEntity );
					s.getTransaction().commit();
				}
				catch (Exception e) {
					s.getTransaction().rollback();
					throw e;
				}
				finally {
					s.close();
				}
			}
			finally {
				try {
					sf.close();
				}
				catch (Exception ignore) {
				}
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "TheEntity")
	@Table(name = "TheEntity")
	public static class TheEntity {
		@Id
		@Column( length = 16 )
		@GeneratedValue
		public UUID id;
	}
}
