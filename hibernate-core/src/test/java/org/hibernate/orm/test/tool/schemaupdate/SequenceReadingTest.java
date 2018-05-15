/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemaupdate;

import java.util.EnumSet;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

/**
 * Regression test fr a bug in org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl
 *
 * @author Steve Ebersole
 * @see
 */
@TestForIssue(jiraKey = "HHH-9745")
public class SequenceReadingTest extends BaseSchemaUnitTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MyEntity.class };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.DIALECT, MyExtendedH2Dialect.class );
	}

	@SchemaTest
	public void testSequenceReading(SchemaScope schemaScope) {
		schemaScope.withSchemaUpdate( schemaUpdate ->
								  schemaUpdate.setHaltOnError( true ).execute( EnumSet.of( TargetType.DATABASE ) ) );
	}

	/**
	 * An integral piece of the bug is Dialects which to not support sequence, so lets trick the test
	 */
	public static class MyExtendedH2Dialect extends H2Dialect {
		@Override
		public SequenceInformationExtractor getSequenceInformationExtractor() {
			return SequenceInformationExtractorNoOpImpl.INSTANCE;
		}

		@Override
		public String getQuerySequencesString() {
			return null;
		}
	}

	@Entity(name = "MyEntity")
	@Table(name = "my_entity")
	public static class MyEntity {
		@Id
		public Integer id;
	}

}
