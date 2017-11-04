/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemafilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.orm.test.schemaupdate.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilter;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.schemafilter.RecordingTarget.Category.SEQUENCE_CREATE;
import static org.hibernate.orm.test.schemafilter.RecordingTarget.Category.SEQUENCE_DROP;


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

		assertThat( target.getActions( SEQUENCE_CREATE ), containsExactly(
				"entity_1_seq_gen",
				"entity_2_seq_gen"
		) );
	}

	@SchemaTest
	public void createSchema_filtered(SchemaScope schemaScope) {
		RecordingTarget target = doCreation( schemaScope, new TestSchemaFilter() );

		assertThat( target.getActions( SEQUENCE_CREATE ), containsExactly(
				"entity_1_seq_gen"
		) );
	}

	@SchemaTest
	public void dropSchema_unfiltered(SchemaScope schemaScope) {
		RecordingTarget target = doDrop( schemaScope, new DefaultSchemaFilter() );

		assertThat( target.getActions( SEQUENCE_DROP ), containsExactly(
				"entity_1_seq_gen",
				"entity_2_seq_gen"
		) );
	}

	@SchemaTest
	public void dropSchema_filtered(SchemaScope schemaScope) {
		RecordingTarget target = doDrop( schemaScope, new TestSchemaFilter() );

		assertThat(
				target.getActions( SEQUENCE_DROP ),
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
		RecordingTarget target = new RecordingTarget();
		schemaScope.withSchemaCreator( filter, schemaCreator -> schemaCreator.doCreation( true, target ) );
		return target;
	}

	private RecordingTarget doDrop(SchemaScope schemaScope, SchemaFilter filter) {
		RecordingTarget target = new RecordingTarget();
		schemaScope.withSchemaDropper( filter, schemaDropper -> schemaDropper.doDrop( true, target ) );
		return target;
	}

	private BaseMatcher<Set<String>> containsExactly(Object... expected) {
		return containsExactly( new HashSet( Arrays.asList( expected ) ) );
	}

	private BaseMatcher<Set<String>> containsExactly(final Set expected) {
		return new BaseMatcher<Set<String>>() {
			@Override
			public boolean matches(Object item) {
				Set set = (Set) item;
				return set.size() == expected.size()
						&& set.containsAll( expected );
			}

			@Override
			public void describeTo(Description description) {
				description.appendText( "Is set containing exactly " + expected );
			}
		};
	}

}
