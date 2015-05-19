/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.enums;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@TestForIssue( jiraKey = "HHH-7780" )
@RequiresDialect( value = H2Dialect.class )
public class UnspecifiedEnumTypeTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "enums/mappings.hbm.xml" };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( Environment.HBM2DDL_AUTO, "" );
	}

	@Before
	public void prepareTable() {
		Session session = openSession();
		dropTable( session );
		createTable( session );
		session.close();
	}

	public void dropTable(Session session) {
		executeUpdateSafety( session, "drop table ENUM_ENTITY if exists" );
	}

	private void createTable(Session session) {
		executeUpdateSafety(
				session,
				"create table ENUM_ENTITY (ID bigint not null, enum1 varchar(255), enum2 integer, primary key (ID))"
		);
	}

	@After
	public void dropTable() {
		dropTable( session );
	}

	@Test
	public void testEnumTypeDiscovery() {
		Session session = openSession();
		session.beginTransaction();
		UnspecifiedEnumTypeEntity entity = new UnspecifiedEnumTypeEntity( UnspecifiedEnumTypeEntity.E1.X, UnspecifiedEnumTypeEntity.E2.A );
		session.persist( entity );
		session.getTransaction().commit();
		session.close();
	}

	private void executeUpdateSafety(Session session, String query) {
		try {
			session.createSQLQuery( query ).executeUpdate();
		}
		catch ( Exception e ) {
		}
	}
}
