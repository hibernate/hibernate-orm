/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lob;
import java.util.List;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.query.Query;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests eager materialization and mutation of long strings.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings( {"UnusedDeclaration"})
public abstract class LongStringTest extends BaseCoreFunctionalTestCase {
	private static final int LONG_STRING_SIZE = 10000;
	private static final String EMPTY = "";

	private final String original = buildRecursively( LONG_STRING_SIZE, 'x' );
	private final String changed = buildRecursively( LONG_STRING_SIZE, 'y' );

	@Test
	public void testBoundedLongStringAccess() {
		Session s = openSession();
		s.beginTransaction();
		LongStringHolder entity = new LongStringHolder();
		s.save( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = s.get( LongStringHolder.class, entity.getId() );
		assertNull( entity.getLongString() );
		entity.setLongString( original );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = s.get( LongStringHolder.class, entity.getId() );
		assertEquals( LONG_STRING_SIZE, entity.getLongString().length() );
		assertEquals( original, entity.getLongString() );
		entity.setLongString( changed );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = s.get( LongStringHolder.class, entity.getId() );
		assertEquals( LONG_STRING_SIZE, entity.getLongString().length() );
		assertEquals( changed, entity.getLongString() );
		entity.setLongString( null );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = s.get( LongStringHolder.class, entity.getId() );
		assertNull( entity.getLongString() );
		entity.setLongString( EMPTY );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		entity = s.get( LongStringHolder.class, entity.getId() );
		if ( entity.getLongString() != null ) {
            if(getDialect() instanceof SybaseASE15Dialect){
                //Sybase uses a single blank to denote an EMPTY string (this is by design). So, when inserting an EMPTY string '', it is interpreted as single blank ' '.
                assertEquals( EMPTY.length(), entity.getLongString().trim().length() );
                assertEquals( EMPTY, entity.getLongString().trim() );
            }else{
			    assertEquals( EMPTY.length(), entity.getLongString().length() );
                assertEquals( EMPTY, entity.getLongString() );
            }
		}
		s.delete( entity );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@SkipForDialect(Oracle8iDialect.class)
	@TestForIssue( jiraKey = "HHH-11477")
	public void testUsingLobPropertyInHqlQuery() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			LongStringHolder entity = new LongStringHolder();
			entity.setLongString( original );
			session.save( entity );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Query query = session.createQuery( "from LongStringHolder where longString = :stringValue" );
			query.setParameter( "stringValue", original );
			final List<LongStringHolder> results = query.list();
			assertThat( results.size(), is( 1 ) );

			assertThat( results.get( 0 ).getLongString(), is( original ) );
		} );
	}

	@Test
	@SkipForDialect(Oracle8iDialect.class)
	@TestForIssue( jiraKey = "HHH-11477")
	public void testSelectLobPropertyInHqlQuery() {
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			LongStringHolder entity = new LongStringHolder();
			entity.setLongString( original );
			session.save( entity );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			final Query query = session.createQuery( "select l.longString from LongStringHolder l where l.longString = :stringValue" );
			query.setParameter( "stringValue", original );
			final List<String> results = query.list();
			assertThat( results.size(), is( 1 ) );

			assertThat( results.get( 0 ), is( original ) );
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	private String buildRecursively(int size, char baseChar) {
		StringBuilder buff = new StringBuilder();
		for( int i = 0; i < size; i++ ) {
			buff.append( baseChar );
		}
		return buff.toString();
	}
}
