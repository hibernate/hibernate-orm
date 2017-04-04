/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.naturalid.inheritance.cache;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@FailureExpected( jiraKey = "HHH-11532" )
public class InheritedNaturalIdCacheTest extends BaseCoreFunctionalTestCase {

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MyEntity.class, ExtendedEntity.class};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	public void testLoadExtendedByNormal() {
		doInHibernate( this::sessionFactory, session -> {
			session.save( new MyEntity( "base" ) );
			session.save( new ExtendedEntity( "extended", "ext" ) );
		});

		doInHibernate( this::sessionFactory, session -> {
			// Sanity check, ensure both entities is accessible.
			MyEntity user = session.byNaturalId( MyEntity.class ).using(
					"uid",
					"base"
			).load();
			ExtendedEntity extendedMyEntity = session.byNaturalId(
					ExtendedEntity.class )
					.using( "uid", "extended" )
					.load();
			assertNotNull( user );
			assertNotNull( extendedMyEntity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			// This throws WrongClassException, since MyEntity was found using the ID, but we wanted ExtendedEntity.
			ExtendedEntity user = session.byNaturalId( ExtendedEntity.class )
					.using( "uid", "base" )
					.load();
			assertNull( user );
		} );
	}

	@Test
	public void testLoadExtendedByNormalCatchingWrongClassException() {
		doInHibernate( this::sessionFactory, session -> {
			session.save( new MyEntity( "normal" ) );
			session.save( new ExtendedEntity( "extended", "ext" ) );
		});

		doInHibernate( this::sessionFactory, session -> {
			MyEntity user = session.byNaturalId( MyEntity.class ).using(
					"uid",
					"normal"
			).load();
			ExtendedEntity extendedMyEntity = session.byNaturalId(
					ExtendedEntity.class )
					.using( "uid", "extended" )
					.load();
			assertNotNull( user );
			assertNotNull( extendedMyEntity );
		} );

		doInHibernate( this::sessionFactory, session -> {
			session.byNaturalId( ExtendedEntity.class ).using(
					"uid",
					"normal"
			).load();
		} );

	}

	@Test
	public void testLoadExtendedByNormalCatchingWrongClassException2() {
		doInHibernate( this::sessionFactory, session -> {
			session.save( new MyEntity( "normal" ) );
			session.save( new ExtendedEntity( "extended", "ext" ) );
		});

		doInHibernate( this::sessionFactory, session -> {
			MyEntity user = session.byNaturalId( MyEntity.class ).using(
					"uid",
					"normal"
			).load();
			ExtendedEntity extendedMyEntity = session.byNaturalId(
					ExtendedEntity.class )
					.using( "uid", "extended" )
					.load();
			assertNotNull( user );
			assertNotNull( extendedMyEntity );
		} );

		// Temporarily change logging level for these two classes to DEBUG
		final Logger afelLogger = LogManager.getLogger(
				"org.hibernate.event.internal.AbstractFlushingEventListener" );
		final Logger epLogger = LogManager.getLogger(
				"org.hibernate.internal.util.EntityPrinter" );
		final Level afelLevel = afelLogger.getLevel();
		final Level epLevel = epLogger.getLevel();

		try {
			// this throws if logging level is set to debug
			doInHibernate( this::sessionFactory, session -> {

				afelLogger.setLevel( Level.DEBUG );
				epLogger.setLevel( Level.DEBUG );

				session.byNaturalId( ExtendedEntity.class ).using(
						"uid",
						"normal"
				).load();
			} );

		}
		finally {
			// set back previous logging level
			afelLogger.setLevel( afelLevel );
			epLogger.setLevel( epLevel );
		}
	}
}
