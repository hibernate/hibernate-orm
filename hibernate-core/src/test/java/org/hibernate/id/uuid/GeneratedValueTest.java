/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.id.uuid;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests a UUID attribute annotated as a generated id value.
 *
 * @author Steve Ebersole
 */
public class GeneratedValueTest extends BaseUnitTestCase {
	@Test
	public void testGeneratedUuidId() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		try {
			Metadata metadata = new MetadataSources( ssr ).addAnnotatedClass( TheEntity.class ).buildMetadata();
			( (MetadataImpl) metadata ).validate();

			PersistentClass entityBinding = metadata.getEntityBinding( TheEntity.class.getName() );
			assertEquals( UUID.class, entityBinding.getIdentifier().getType().getReturnedClass() );
			IdentifierGenerator generator = entityBinding.getIdentifier().createIdentifierGenerator(
					metadata.getIdentifierGeneratorFactory(),
					metadata.getDatabase().getDialect(),
					null,
					null,
					(RootClass) entityBinding
			);
			assertTyping( UUIDGenerator.class, generator );

			// now a functional test
			SessionFactory sf = metadata.buildSessionFactory();
			try {
				TheEntity theEntity = new TheEntity();

				Session s = sf.openSession();
				s.beginTransaction();
				s.save( theEntity );
				s.getTransaction().commit();
				s.close();

				assertNotNull( theEntity.id );

				s = sf.openSession();
				s.beginTransaction();
				s.delete( theEntity );
				s.getTransaction().commit();
				s.close();
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
		@GeneratedValue
		public UUID id;
	}
}
