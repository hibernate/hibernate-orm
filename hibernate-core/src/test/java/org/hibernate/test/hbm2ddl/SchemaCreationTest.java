/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.hbm2ddl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.metamodel.source.internal.SimpleEntity;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SchemaCreationTest extends BaseUnitTestCase {
	public BasicServiceRegistry buildServiceRegistry() {
		Properties cfg = Environment.getProperties();
		cfg.setProperty( Environment.HBM2DDL_AUTO, "create" );
		return ServiceRegistryBuilder.buildServiceRegistry( cfg );
	}

	private MetadataImplementor buildMetadata(BasicServiceRegistry serviceRegistry) {
		MetadataSources sources = new MetadataSources( serviceRegistry );
		sources.addAnnotatedClass( SimpleEntity.class );
		return (MetadataImplementor) sources.buildMetadata();
	}

	private SessionFactory buildSessionFactory() {
		return buildMetadata( buildServiceRegistry() ).buildSessionFactory();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-6384")
	public void testSchemaDropBeforeCreation() {
		// Create database schema for the first time.
		SessionFactory firstSessionFactory = buildSessionFactory();
		generateSampleData( firstSessionFactory );
		Assert.assertEquals( 1, getSampleDataSize( firstSessionFactory ) );

		// Create database schema. Previously created table shall be dropped.
		SessionFactory secondSessionFactory = buildSessionFactory();
		Assert.assertEquals( 0, getSampleDataSize( secondSessionFactory ) );

		firstSessionFactory.close();
		secondSessionFactory.close();
	}

	private int getSampleDataSize(SessionFactory sessionFactory) {
		Session session = sessionFactory.openSession();
		int size = session.createCriteria( SimpleEntity.class ).list().size();
		session.close();
		return size;
	}

	private void generateSampleData(SessionFactory sessionFactory) {
		Session session = sessionFactory.openSession();
		Transaction transaction = session.getTransaction();
		transaction.begin();
		SimpleEntity simpleEntity = new SimpleEntity();
		simpleEntity.setId( 1L );
		simpleEntity.setName( "Kinga" );
		session.save( simpleEntity );
		transaction.commit();
		session.close();
	}
}
