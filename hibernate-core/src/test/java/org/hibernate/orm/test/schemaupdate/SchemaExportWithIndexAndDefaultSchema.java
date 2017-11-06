/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9866")
@RequiresDialect(dialectClass = PostgreSQL81Dialect.class, matchSubTypes = true)
public class SchemaExportWithIndexAndDefaultSchema extends BaseSchemaUnitTestCase {


	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( Environment.GLOBALLY_QUOTED_IDENTIFIERS, "true" )
				.applySetting( Environment.DEFAULT_SCHEMA, "public" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected void beforeEach(SchemaScope scope) {
		scope.withSchemaExport( schemaExport ->
										schemaExport.create( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) ) );
	}

	@SchemaTest
	public void shouldCreateIndex(SchemaScope scope) {
		scope.withSchemaExport( schemaExport -> {
			schemaExport.create( EnumSet.of( TargetType.DATABASE, TargetType.STDOUT ) );
			assertThat( schemaExport.getExceptions().size(), is( 0 ) );
		} );
	}

	@Entity
	@Table(name = "MyEntity", indexes = { @Index(columnList = "id", name = "user_id_hidx") })
	public static class MyEntity {
		private int id;

		@Id
		public int getId() {
			return this.id;
		}

		public void setId(final int id) {
			this.id = id;
		}
	}
}
