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

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import static org.hibernate.orm.test.schemafilter.RecordingTarget.Category.SEQUENCE_CREATE;
import static org.hibernate.orm.test.schemafilter.RecordingTarget.Category.SEQUENCE_DROP;


/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-10937")
@RequiresDialectFeature(value = { DialectChecks.SupportSchemaCreation.class })
public class SequenceFilterTest extends BaseSchemaUnitTestCase {

	@Override
	protected void applySettings(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.applySetting( AvailableSettings.DIALECT, H2Dialect.class.getName() );
		serviceRegistryBuilder.applySetting( AvailableSettings.FORMAT_SQL, false );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Schema1Entity1.class, Schema2Entity2.class };
	}

	@Test
	public void createSchema_unfiltered() {
		RecordingTarget target = doCreation( new DefaultSchemaFilter() );

		Assert.assertThat( target.getActions( SEQUENCE_CREATE ), containsExactly(
				"entity_1_seq_gen",
				"entity_2_seq_gen"
		) );
	}

	@Test
	public void createSchema_filtered() {
		RecordingTarget target = doCreation( new TestSchemaFilter() );

		Assert.assertThat( target.getActions( SEQUENCE_CREATE ), containsExactly(
				"entity_1_seq_gen"
		) );
	}

	@Test
	public void dropSchema_unfiltered() {
		RecordingTarget target = doDrop( new DefaultSchemaFilter() );

		Assert.assertThat( target.getActions( SEQUENCE_DROP ), containsExactly(
				"entity_1_seq_gen",
				"entity_2_seq_gen"
		) );
	}

	@Test
	public void dropSchema_filtered() {
		RecordingTarget target = doDrop( new TestSchemaFilter() );

		Assert.assertThat(
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

	private RecordingTarget doCreation(SchemaFilter filter) {
		RecordingTarget target = new RecordingTarget();
		createSchemaCreator( filter ).doCreation( true, target );
		return target;
	}

	private RecordingTarget doDrop(SchemaFilter filter) {
		RecordingTarget target = new RecordingTarget();
		createSchemaDropper( filter ).doDrop( true, target );
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
