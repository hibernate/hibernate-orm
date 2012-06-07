/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.stateless.fetching;

import java.util.Date;

import org.jboss.logging.Logger;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.DefaultNamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class StatelessSessionFetchingTest extends BaseCoreFunctionalTestCase {
	private static final Logger log = Logger.getLogger( StatelessSessionFetchingTest.class );

	@Override
	public String[] getMappings() {
		return new String[] { "stateless/fetching/Mappings.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategy( new TestingNamingStrategy() );
	}

	private class TestingNamingStrategy extends DefaultNamingStrategy {
		private final String prefix = determineUniquePrefix();

		protected String applyPrefix(String baseTableName) {
			String prefixed = prefix + '_' + baseTableName;
            log.debug("prefixed table name : " + baseTableName + " -> " + prefixed);
			return prefixed;
		}

		@Override
		public String classToTableName(String className) {
			return applyPrefix( super.classToTableName( className ) );
		}

		@Override
		public String tableName(String tableName) {
			if ( tableName.startsWith( "`" ) && tableName.endsWith( "`" ) ) {
				return tableName;
			}
			if ( tableName.startsWith( prefix + '_' ) ) {
				return tableName;
			}
			return applyPrefix( tableName );
		}

		@Override
		public String collectionTableName(String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable, String propertyName) {
			String tableName = super.collectionTableName( ownerEntity, ownerEntityTable, associatedEntity, associatedEntityTable, propertyName );
			return applyPrefix( tableName );
		}

		@Override
		public String logicalCollectionTableName(String tableName, String ownerEntityTable, String associatedEntityTable, String propertyName) {
			String resolvedTableName = prefix + '_' + super.logicalCollectionTableName( tableName, ownerEntityTable, associatedEntityTable, propertyName );
			System.out.println( "Logical collection table name : " + tableName + " -> " + resolvedTableName );
			return resolvedTableName;
		}

		private String determineUniquePrefix() {
			return StringHelper.collapseQualifier( getClass().getName(), false ).toUpperCase();
		}
	}

	@Test
	public void testDynamicFetch() {
		Session s = openSession();
		s.beginTransaction();
		Date now = new Date();
		User me = new User( "me" );
		User you = new User( "you" );
		Resource yourClock = new Resource( "clock", you );
		Task task = new Task( me, "clean", yourClock, now ); // :)
		s.save( me );
		s.save( you );
		s.save( yourClock );
		s.save( task );
		s.getTransaction().commit();
		s.close();

		StatelessSession ss = sessionFactory().openStatelessSession();
		ss.beginTransaction();
		Task taskRef = ( Task ) ss.createQuery( "from Task t join fetch t.resource join fetch t.user" ).uniqueResult();
		assertTrue( taskRef != null );
		assertTrue( Hibernate.isInitialized( taskRef ) );
		assertTrue( Hibernate.isInitialized( taskRef.getUser() ) );
		assertTrue( Hibernate.isInitialized( taskRef.getResource() ) );
		assertFalse( Hibernate.isInitialized( taskRef.getResource().getOwner() ) );
		ss.getTransaction().commit();
		ss.close();

		cleanup();
	}

	private void cleanup() {
		Session s = openSession();
		s.beginTransaction();
		s.createQuery( "delete Task" ).executeUpdate();
		s.createQuery( "delete Resource" ).executeUpdate();
		s.createQuery( "delete User" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}
