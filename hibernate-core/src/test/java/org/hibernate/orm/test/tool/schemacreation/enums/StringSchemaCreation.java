/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemacreation.enums;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.orm.test.tool.util.RecordingTarget;
import org.hibernate.tool.schema.internal.exec.GenerationTargetToStdout;

import org.hibernate.testing.junit5.RequiresDialect;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Andrea Boriero
 */
@RequiresDialect(dialectClass = H2Dialect.class, matchSubTypes = true)
public class StringSchemaCreation extends BaseSchemaUnitTestCase {
	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, false );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Override
	protected boolean createSqlScriptTempOutputFile() {
		return true;
	}

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@SchemaTest
	public void testElementAndCollectionTableAreCreated(SchemaScope scope) {
		final RecordingTarget target = new RecordingTarget( getDialect() );
		scope.withSchemaCreator(
				null,
				schemaCreator -> schemaCreator.doCreation(
						true,
						target,
						new GenerationTargetToStdout()
				)
		);

		assertThat(
				target.getActions( target.tableCreateActions() ),
				target.containsExactly(
						"person (gender varchar(255), id bigint not null, name varchar(255), primary key (id))"
				)
		);
	}

	@Entity(name = "Person")
	@Table(name = "person")
	public static class Person {
		@Id
		public long id;

		String name;

		@Enumerated(EnumType.STRING)
		OrdinalSchemaCreation.Gender gender;
	}

	public enum Gender {
		MALE,
		FEMALE
	}
}
