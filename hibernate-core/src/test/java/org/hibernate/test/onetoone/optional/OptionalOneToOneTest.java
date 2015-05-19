/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.onetoone.optional;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNull;

/**
 * @author Gavin King
 */
public class OptionalOneToOneTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "onetoone/optional/Person.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false");
		cfg.setProperty(Environment.GENERATE_STATISTICS, "true");
	}

	@Test
	public void testOptionalOneToOneRetrieval() {
		Session s = openSession();
		s.beginTransaction();
		Person me = new Person();
		me.name = "Steve";
		s.save( me );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		me = ( Person ) s.load( Person.class, me.name );
		assertNull( me.address );
		s.delete( me );
		s.getTransaction().commit();
		s.close();
	}
}
