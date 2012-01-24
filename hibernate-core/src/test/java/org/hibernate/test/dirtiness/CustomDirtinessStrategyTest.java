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
package org.hibernate.test.dirtiness;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.junit.Test;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
public class CustomDirtinessStrategyTest extends BaseCoreFunctionalTestCase {
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.getProperties().put( AvailableSettings.CUSTOM_ENTITY_DIRTINESS_STRATEGY, Strategy.INSTANCE );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Thing.class };
	}

	@Test
	public void testCustomStrategy() throws Exception {
		final String INITIAL_NAME = "thing 1";
		final String SUBSEQUENT_NAME = "thing 2";

		Session session = openSession();
		session.beginTransaction();
		session.save( new Thing( INITIAL_NAME ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		Thing thing = (Thing) session.get( Thing.class, 1L );
		thing.setName( SUBSEQUENT_NAME );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		thing = (Thing) session.get( Thing.class, 1L );
		assertEquals( INITIAL_NAME, thing.getName() );
		session.delete( thing );
		session.getTransaction().commit();
		session.close();
	}

	public static class Strategy implements CustomEntityDirtinessStrategy {
		public static final Strategy INSTANCE = new Strategy();

		@Override
		public boolean canDirtyCheck(Object entity, Session session) {
			return true;
		}

		@Override
		public boolean isDirty(Object entity, Session session) {
			return false;
		}

		@Override
		public void resetDirty(Object entity, Session session) {
		}
	}

}
