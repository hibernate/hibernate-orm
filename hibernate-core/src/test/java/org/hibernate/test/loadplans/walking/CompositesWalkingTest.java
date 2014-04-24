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
package org.hibernate.test.loadplans.walking;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.walking.spi.MetamodelGraphWalker;

import org.hibernate.test.annotations.collectionelement.LocalizedString;
import org.hibernate.test.annotations.collectionelement.TestCourse;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class CompositesWalkingTest extends BaseUnitTestCase {
	/**
	 * Test one-level composites defined as part of an entity.
	 */
	@Test
	public void testEntityComposite() {
		final SessionFactory sf = new Configuration()
				.addAnnotatedClass( TestCourse.class )
				.addAnnotatedClass( LocalizedString.class )
				.buildSessionFactory();
		try {
			final EntityPersister ep = (EntityPersister) sf.getClassMetadata( TestCourse.class );
			MetamodelGraphWalker.visitEntity( new LoggingAssociationVisitationStrategy(), ep );
		}
		finally {
			sf.close();
		}
	}
}
