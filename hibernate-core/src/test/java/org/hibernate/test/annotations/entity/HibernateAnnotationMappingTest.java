/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
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
package org.hibernate.test.annotations.entity;

import java.util.ConcurrentModificationException;

import org.junit.Test;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.fail;

/**
 * @author Guenther Demetz
 */
public class HibernateAnnotationMappingTest extends BaseUnitTestCase {

	@Test
	@TestForIssue(jiraKey = "HHH-7446")
	public void testUniqueConstraintAnnotationOnNaturalIds() throws Exception {
		Configuration configuration = new Configuration();
		configuration.setProperty( Environment.HBM2DDL_AUTO, "create-drop" );
		ServiceRegistry serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( configuration.getProperties() );
		SessionFactory sf = null;
		try {
			if ( isMetadataUsed() ) {
				MetadataSources metadataSources = new MetadataSources( serviceRegistry );
				sf = metadataSources.addAnnotatedClass( Month.class ).buildMetadata().buildSessionFactory();

			}
			else {
				configuration.addAnnotatedClass( Month.class );
				sf = configuration.buildSessionFactory();

			}
		}
		catch ( ConcurrentModificationException e ) {
			fail( e.toString() );
		}
		finally {
			if( sf != null ){
				sf.close();
			}
			if( serviceRegistry != null ){
				ServiceRegistryBuilder.destroy( serviceRegistry );
			}
		}

	}
}
