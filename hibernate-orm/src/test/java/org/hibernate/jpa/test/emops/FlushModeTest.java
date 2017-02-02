/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
