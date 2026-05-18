/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.id.uuid.UuidGenerator;
import org.hibernate.mapping.RootClass;
import org.hibernate.orm.test.idgen.GeneratorSettingsImpl;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hibernate.cfg.SchemaToolingSettings.HBM2DDL_AUTO;
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
	public void testGeneratedUuidId() {
		var ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( HBM2DDL_AUTO, "create-drop" )
				.build();
		try {
			var metadata = (MetadataImplementor)
					new MetadataSources( ssr )
							.addAnnotatedClass( TheEntity.class )
							.buildMetadata();
			metadata.orderColumns( false );
			metadata.validate();

			var entityBinding = metadata.getEntityBinding( TheEntity.class.getName() );
			assertEquals( UUID.class, entityBinding.getIdentifier().getType().getReturnedClass() );
			var keyValue = entityBinding.getIdentifier();
			var database = metadata.getDatabase();
			final var generator =
					keyValue.createGenerator( database.getDialect(), (RootClass) entityBinding,
							entityBinding.getIdentifierProperty(), new GeneratorSettingsImpl( metadata ) );
			assertTyping( UuidGenerator.class, generator );

			// now a functional test
			try (var sf = metadata.buildSessionFactory()) {
				TheEntity theEntity = new TheEntity();

				var s = sf.openSession();
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
			catch (Exception ignore) {
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
