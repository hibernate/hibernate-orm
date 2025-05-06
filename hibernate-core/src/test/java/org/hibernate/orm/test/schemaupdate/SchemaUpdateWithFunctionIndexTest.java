/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;
import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
@JiraKey(value = "HHH-10191")
@RequiresDialect(PostgreSQLDialect.class)
public class SchemaUpdateWithFunctionIndexTest extends BaseNonConfigCoreFunctionalTestCase {
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Test
	public void testUpdateSchema() {
		new SchemaUpdate().execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata );
	}

	@Before
	public void setUp() {
		dropFunctionIndex();
		dropTable();
		createTable();
		createFunctionIndex();
		serviceRegistry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "false" )
				.applySetting( Environment.DEFAULT_SCHEMA, "public" )
				.build();
		metadata = (MetadataImplementor) new MetadataSources( serviceRegistry )
				.addAnnotatedClass( MyEntity.class )
				.buildMetadata();
	}

	private void createTable() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createNativeQuery( "CREATE TABLE MyEntity(id bigint, name varchar(255));" );
		query.executeUpdate();
		transaction.commit();
		session.close();
	}

	private void createFunctionIndex() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createNativeQuery( "CREATE UNIQUE INDEX uk_MyEntity_name_lowercase ON MyEntity (lower(name));" );
		query.executeUpdate();
		transaction.commit();
		session.close();
	}

	private void dropTable() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createNativeQuery( "DROP TABLE IF EXISTS MyEntity;" );
		query.executeUpdate();
		transaction.commit();
		session.close();
	}

	private void dropFunctionIndex() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createNativeQuery( "DROP INDEX IF EXISTS uk_MyEntity_name_lowercase;" );
		query.executeUpdate();
		transaction.commit();
		session.close();
	}

	@After
	public void tearDown() {
		dropFunctionIndex();
		dropTable();
		new SchemaExport().drop( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ), metadata );
		StandardServiceRegistryBuilder.destroy( serviceRegistry );
	}

	@Entity
	@Table(name = "MyEntity", indexes = @Index(columnList = "otherInfo"))
	public static class MyEntity {

		private int id;

		private String name;

		private int otherInfo;

		@Id
		public int getId() {
			return this.id;
		}

		public void setId(final int id) {
			this.id = id;
		}

		@Basic
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Basic
		public int getOtherInfo() {
			return otherInfo;
		}

		public void setOtherInfo(int otherInfo) {
			this.otherInfo = otherInfo;
		}
	}
}
