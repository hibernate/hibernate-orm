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
