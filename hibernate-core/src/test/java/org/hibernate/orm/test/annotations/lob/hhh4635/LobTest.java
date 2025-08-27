/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.lob.hhh4635;

import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.Hibernate.getLobHelper;

/**
 * To reproduce this issue, Oracle MUST use a multi-byte character set (UTF-8)!
 *
 * @author Brett Meyer
 */
@RequiresDialect( OracleDialect.class )
@JiraKey( value = "HHH-4635" )
public class LobTest extends BaseCoreFunctionalTestCase {

	@Test
	public void hibernateTest() {
		printConfig();

		Session session = openSession();
		session.beginTransaction();
		LobTestEntity entity = new LobTestEntity();
		entity.setId(1L);
		entity.setLobValue(getLobHelper().createBlob(new byte[9999]));
		entity.setQwerty(randomString(4000));
		session.persist(entity);
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { LobTestEntity.class };
	}

	private String randomString( int count ) {
		StringBuilder buffer = new StringBuilder(count);
		for( int i = 0; i < count; i++ ) {
			buffer.append( 'a' );
		}
		return buffer.toString();
	}

	private void printConfig() {
		String sql = "select value from V$NLS_PARAMETERS where parameter = 'NLS_CHARACTERSET'";

		Session session = openSession();
		session.beginTransaction();
		Query query = session.createNativeQuery( sql );

		String s = (String) query.uniqueResult();
		log.debug( "Using Oracle charset " + s );
		session.getTransaction().commit();
		session.close();
	}
}
