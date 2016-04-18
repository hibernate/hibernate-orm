/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HHH-10191")
@RequiresDialect(PostgreSQL81Dialect.class)
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
		serviceRegistry = new StandardServiceRegistryBuilder()
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
		Query query = session.createSQLQuery( "CREATE TABLE MyEntity(id bigint, name varchar(255));" );
		query.executeUpdate();
		transaction.commit();
		session.close();
	}

	private void createFunctionIndex() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createSQLQuery( "CREATE UNIQUE INDEX uk_MyEntity_name_lowercase ON MyEntity (lower(name));" );
		query.executeUpdate();
		transaction.commit();
		session.close();
	}

	private void dropTable() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createSQLQuery( "DROP TABLE IF EXISTS MyEntity;" );
		query.executeUpdate();
		transaction.commit();
		session.close();
	}

	private void dropFunctionIndex() {
		Session session = openSession();
		Transaction transaction = session.beginTransaction();
		Query query = session.createSQLQuery( "DROP INDEX IF EXISTS uk_MyEntity_name_lowercase;" );
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
