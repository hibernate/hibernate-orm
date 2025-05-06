/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand.quotedidentifiers;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.DialectCheck;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test of various situations with native-sql queries and quoted identifiers
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature( value = NativeSqlAndQuotedIdentifiersTest.LocalDialectCheck.class )
public class NativeSqlAndQuotedIdentifiersTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "sql/hand/quotedidentifiers/Mappings.hbm.xml" };
	}

	public static class LocalDialectCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return '\"' == dialect.openQuote();
		}
	}

	@Override
	protected void prepareTest() throws Exception {
		if( sessionFactory()==null)return;
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		session.persist( new Person( "me" ) );
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected void cleanupTest() throws Exception {
		if( sessionFactory()==null)return;
		Session session = sessionFactory().openSession();
		session.beginTransaction();
		session.createQuery( "delete Person" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testCompleteScalarDiscovery() {
		Session session = openSession();
		session.beginTransaction();
		session.getNamedQuery( "query-person" ).list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testPartialScalarDiscovery() {
		Session session = openSession();
		session.beginTransaction();
		NativeQuery query = session.getNamedNativeQuery( "query-person", "person-scalar" );
		query.list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testBasicEntityMapping() {
		Session session = openSession();
		session.beginTransaction();
		NativeQuery query = session.getNamedNativeQuery( "query-person", "person-entity-basic" );
		query.list();
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testExpandedEntityMapping() {
		Session session = openSession();
		session.beginTransaction();
		NativeQuery query = session.getNamedNativeQuery( "query-person", "person-entity-expanded" );
		query.list();
		session.getTransaction().commit();
		session.close();
	}
}
