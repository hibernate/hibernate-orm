/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.test.emops;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

/**
 * @author Emmanuel Bernard
 */
public class FlushModeTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testCreateEMFlushMode() throws Exception {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put( "org.hibernate.flushMode", "manual" );
		EntityManager em = createEntityManager( properties );
		em.getTransaction().begin();
		Dress dress = new Dress();
		dress.name = "long dress";
		em.persist( dress );
		em.getTransaction().commit();

		em.clear();

		Assert.assertNull( em.find( Dress.class, dress.name ) );

		em.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Race.class,
				Competitor.class,
				Dress.class
		};
	}
}
