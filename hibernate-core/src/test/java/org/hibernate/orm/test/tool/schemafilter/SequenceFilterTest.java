/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tool.schemafilter;

import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.orm.test.tool.BaseSchemaUnitTestCase;
import org.hibernate.orm.test.tool.util.RecordingTarget;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilter;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.tool.util.RecordingTarget.containsExactly;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10937")
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportSchemaCreation.class)
public class SequenceFilterTest extends BaseSchemaUnitTestCase {

	@Override
	protected boolean dropSchemaAfterTest() {
		return false;
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, false );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Schema1Entity1.class, Schema2Entity2.class };
	}

	@SchemaTest
	public void createSchema_unfiltered(SchemaScope schemaScope) {
		RecordingTarget target = doCreation( schemaScope, new DefaultSchemaFilter() );

		assertThat(
				target.getActions( target.sequenceNameCreateActions() ),
				containsExactly( "entity_1_seq_gen", "entity_2_seq_gen" )
		);
	}

	@SchemaTest
	public void createSchema_filtered(SchemaScope schemaScope) {
		RecordingTarget target = doCreation( schemaScope, new TestSchemaFilter() );

		assertThat(
				target.getActions( target.sequenceNameCreateActions() ),
				containsExactly( "entity_1_seq_gen" )
		);
	}

	@SchemaTest
	public void dropSchema_unfiltered(SchemaScope schemaScope) {
		RecordingTarget target = doDrop( schemaScope, new DefaultSchemaFilter() );

		assertThat(
				target.getActions( target.sequenceNameDropActions() ),
				containsExactly( "entity_1_seq_gen", "entity_2_seq_gen" )
		);
	}

	@SchemaTest
	public void dropSchema_filtered(SchemaScope schemaScope) {
		RecordingTarget target = doDrop( schemaScope, new TestSchemaFilter() );

		Set<String> actions = target.getActions( target.sequenceNameDropActions() );
		assertThat(
				actions,
				containsExactly( "entity_1_seq_gen" )
		);
	}

	@Entity
	@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "entity_1_seq_gen")
	@Table(name = "the_entity_1", schema = "the_schema_1")
	public static class Schema1Entity1 {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idgen")
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	@SequenceGenerator(initialValue = 1, name = "idgen2", sequenceName = "entity_2_seq_gen")
	@Table(name = "the_entity_2", schema = "the_schema_2")
	public static class Schema2Entity2 {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idgen2")
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	private static class TestSchemaFilter implements SchemaFilter {
		@Override
		public boolean includeNamespace(Namespace namespace) {
			return true;
		}

		@Override
		public boolean includeTable(ExportableTable table) {
			return true;
		}

		@Override
		public boolean includeSequence(org.hibernate.metamodel.model.relational.spi.Sequence sequence) {
			final String render = sequence.getName().render();
			return !"entity_2_seq_gen".endsWith( sequence.getName().render() );
		}
	}

	private RecordingTarget doCreation(SchemaScope schemaScope, SchemaFilter filter) {
		RecordingTarget target = new RecordingTarget( getDialect() );
		schemaScope.withSchemaCreator( filter, schemaCreator -> schemaCreator.doCreation( true, target ) );
		return target;
	}

	private RecordingTarget doDrop(SchemaScope schemaScope, SchemaFilter filter) {
		RecordingTarget target = new RecordingTarget( getDialect() );
		schemaScope.withSchemaDropper( filter, schemaDropper -> schemaDropper.doDrop( true, target ) );
		return target;
	}
}
