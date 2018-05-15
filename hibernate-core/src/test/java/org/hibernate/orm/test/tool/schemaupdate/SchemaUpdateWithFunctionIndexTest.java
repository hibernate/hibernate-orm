/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;
import org.junit.jupiter.api.Disabled;

/**
 * @author Yoann Rodiere
 */
@TestForIssue(jiraKey = "HHH-10191")
@RequiresDialect(dialectClass = PostgreSQL81Dialect.class, matchSubTypes = true)

public class SchemaUpdateWithFunctionIndexTest extends BaseSchemaUnitTestCase {
	protected ServiceRegistry serviceRegistry;
	protected MetadataImplementor metadata;

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "false" )
				.applySetting( Environment.DEFAULT_SCHEMA, "public" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	public void beforeEach(SchemaScope scope){
		executeSqlStatement( "DROP TABLE IF EXISTS MyEntity;" );
		executeSqlStatement( "DROP INDEX IF EXISTS uk_MyEntity_name_lowercase;");
		executeSqlStatement( "CREATE TABLE MyEntity(id bigint, name varchar(255));" );
		executeSqlStatement( "CREATE UNIQUE INDEX uk_MyEntity_name_lowercase ON MyEntity (lower(name));");
	}

	@Override
	protected void afterEach(SchemaScope scope) {
		executeSqlStatement( "DROP TABLE IF EXISTS MyEntity;" );
		executeSqlStatement( "DROP INDEX IF EXISTS uk_MyEntity_name_lowercase;");
	}

	@SchemaTest
	@Disabled
	public void testUpdateSchema(SchemaScope scope) {
		scope.withSchemaUpdate( schemaUpdate ->
										schemaUpdate.execute( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) ) );
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
