/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.converter;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.internal.MetadataImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.envers.test.AbstractEnversTest;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class BasicModelingTest extends AbstractEnversTest {
	@Test
	@TestForIssue( jiraKey = "HHH-9042" )
	public void testMetamodelBuilding() {
		StandardServiceRegistry ssr = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.build();
		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( Person.class )
					.getMetadataBuilder()
					.applyAttributeConverter( SexConverter.class )
					.build();

			( (MetadataImpl) metadata ).validate();

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
