/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.metamodel;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.ManagedType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.junit.Assert.assertNotNull;

/**
 * Specifically see if we can access a MappedSuperclass via Metamodel that is not part of a entity hierarchy
 *
 * @author Steve Ebersole
 */
public class MappedSuperclassType2Test extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-8534" )
	@FailureExpected( jiraKey = "HHH-8534" )
	public void testMappedSuperclassAccessNoEntity() {
		// stupid? yes.  tck does it? yes.

		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				// pass in a MappedSuperclass that is not used in any entity hierarchy
				return Arrays.asList( SomeMappedSuperclass.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		try {
			ManagedType<SomeMappedSuperclass> type = emf.getMetamodel().managedType( SomeMappedSuperclass.class );
			// the issue was in regards to throwing an exception, but also check for nullness
			assertNotNull( type );
		}
		finally {
			emf.close();
		}
	}
}
