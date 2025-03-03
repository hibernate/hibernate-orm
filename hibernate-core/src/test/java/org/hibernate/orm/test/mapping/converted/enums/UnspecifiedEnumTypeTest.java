/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jdbc.Work;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Lukasz Antoniak
 */
@JiraKey( value = "HHH-7780" )
@RequiresDialect( value = H2Dialect.class )
public class UnspecifiedEnumTypeTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String getBaseForMappings() {
		return "";
	}

	@Override
	protected String[] getMappings() {
		return new String[] { "org/hibernate/orm/test/mapping/converted/enums/mappings.hbm.xml" };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( Environment.HBM2DDL_AUTO, "" );
		configuration.setProperty( Environment.PREFER_NATIVE_ENUM_TYPES, "false" );
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
		Session session = openSession();
		dropTable( session );
		session.close();
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
		session.doWork(
				new Work() {
					@Override
					public void execute(Connection connection) throws SQLException {
						connection.createStatement().execute( query );
					}
				}
		);
	}
}
