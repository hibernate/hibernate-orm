/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import org.hibernate.Session;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Lukasz Antoniak
 */
@JiraKey( value = "HHH-7780" )
@RequiresDialect( value = H2Dialect.class )
@DomainModel(xmlMappings = {"org/hibernate/orm/test/mapping/converted/enums/mappings.hbm.xml"})
@SessionFactory(exportSchema = false)
@ServiceRegistry(settings = {@Setting(name = Environment.PREFER_NATIVE_ENUM_TYPES, value = "false")})
public class UnspecifiedEnumTypeTest {

	@BeforeEach
	public void prepareTable(SessionFactoryScope scope) {
		scope.inSession( session -> {
			dropTable( session );
			createTable( session );
		} );
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

	@AfterEach
	public void dropTable(SessionFactoryScope scope) {
		scope.inSession( this::dropTable );
	}

	@Test
	public void testEnumTypeDiscovery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.persist(new UnspecifiedEnumTypeEntity(UnspecifiedEnumTypeEntity.E1.X, UnspecifiedEnumTypeEntity.E2.A) )
		);
	}

	private void executeUpdateSafety(Session session, String query) {
		session.doWork(
				connection -> connection.createStatement().execute( query )
		);
	}
}
