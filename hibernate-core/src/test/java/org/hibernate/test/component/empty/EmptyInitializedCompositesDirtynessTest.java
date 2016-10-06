/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.test.component.empty;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test class for empty embedded dirtiness computation.
 *
 * @author Laurent Almeras
 */
public class EmptyInitializedCompositesDirtynessTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ComponentEmptyEmbeddedOwner.class, ComponentEmptyEmbedded.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( Environment.CREATE_EMPTY_COMPOSITES_ENABLED, Boolean.valueOf( true ) );
	}

	/**
	 * Test for dirtyness computation consistency when a property is an empty composite and that empty composite
	 * initialization is set.
	 */
	@Test
	@TestForIssue(jiraKey = "HHH-7610")
	public void testInitializedCompositesEmpty() {
		Session s = openSession();
		s.getTransaction().begin();

		ComponentEmptyEmbeddedOwner owner = new ComponentEmptyEmbeddedOwner();
		s.persist( owner );

		s.flush();
		s.getTransaction().commit();

		s.clear();
		s.getTransaction().begin();
		owner = (ComponentEmptyEmbeddedOwner) s.get( ComponentEmptyEmbeddedOwner.class, owner.getId() );
		assertNotNull( owner.getEmbedded() );
		assertFalse( s.isDirty() );

		s.getTransaction().rollback();
	}
}
