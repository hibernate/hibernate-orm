/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.metagen.mappedsuperclass.embedded;

import java.util.Arrays;
import javax.persistence.EntityManagerFactory;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.TestingEntityManagerFactoryGenerator;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class MappedSuperclassWithEmbeddedTest extends BaseUnitTestCase {
	@Test
	@TestForIssue( jiraKey = "HHH-5024" )
	public void testStaticMetamodel() {

		// spent too much time digging into this.  problem is ultimately that metamodel binding
		// is not handling MappedSuperclass well

		EntityManagerFactory emf = TestingEntityManagerFactoryGenerator.generateEntityManagerFactory(
				AvailableSettings.LOADED_CLASSES,
				Arrays.asList( Company.class, Address.class )
		);

		assertNotNull( "'Company_.id' should not be null)", Company_.id );
		assertNotNull( "'Company_.address' should not be null)", Company_.address );

		assertNotNull( "'AbstractAddressable_.address' should not be null)", AbstractAddressable_.address );

		emf.close();
	}
}
