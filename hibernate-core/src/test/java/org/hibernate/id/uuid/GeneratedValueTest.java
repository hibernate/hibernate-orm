/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id.uuid;

import java.util.UUID;
import javax.persistence.Column;
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
	public void testGeneratedUuidId() throws Exception {
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
				try {
					s.delete( theEntity );
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
