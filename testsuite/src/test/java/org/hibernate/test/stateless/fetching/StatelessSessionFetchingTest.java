/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
package org.hibernate.test.stateless.fetching;

import java.util.Date;

import junit.framework.Test;

import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.hibernate.cfg.DefaultNamingStrategy;
import org.hibernate.util.StringHelper;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Hibernate;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class StatelessSessionFetchingTest extends FunctionalTestCase {
	public StatelessSessionFetchingTest(String name) {
		super( name );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( StatelessSessionFetchingTest.class );
	}

	public String[] getMappings() {
		return new String[] { "stateless/fetching/Mappings.hbm.xml" };
	}

	// trying a new thing here in tests with this naming strategy to help alleviate table name clashes

	private class TestingNamingStrategy extends DefaultNamingStrategy {
		private final String prefix = determineUniquePrefix();

		@Override
		public String classToTableName(String className) {
			String resolvedTableName = prefix + '_' + super.classToTableName( className );
			System.out.println( "Entity table name : " + className + " -> " + resolvedTableName );
			return resolvedTableName;
		}

		@Override
		public String tableName(String tableName) {
			String resolvedTableName =  prefix + '_' + super.tableName( tableName );
			System.out.println( "Qualified table-name : " + tableName + " -> " + resolvedTableName );
			return resolvedTableName;
		}

		@Override
		public String collectionTableName(String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable, String propertyName) {
			String resolvedTableName = prefix + '_' + super.collectionTableName( ownerEntity, ownerEntityTable, associatedEntity, associatedEntityTable, propertyName );
			System.out.println( "Collection table name : " + ownerEntity + '.' + propertyName + " -> " + resolvedTableName );
			return resolvedTableName;
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

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategy( new TestingNamingStrategy() );
	}

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

		StatelessSession ss = sfi().openStatelessSession();
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
