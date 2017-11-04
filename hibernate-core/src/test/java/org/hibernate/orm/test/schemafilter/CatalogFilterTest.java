package org.hibernate.orm.test.schemafilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.Sequence;
import org.hibernate.naming.Identifier;
import org.hibernate.orm.test.schemaupdate.BaseSchemaUnitTestCase;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilter;

import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.hibernate.testing.junit5.schema.SchemaScope;
import org.hibernate.testing.junit5.schema.SchemaTest;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.orm.test.schemafilter.RecordingTarget.Category.TABLE_CREATE;
import static org.hibernate.orm.test.schemafilter.RecordingTarget.Category.TABLE_DROP;
import static org.hibernate.orm.test.schemafilter.RecordingTarget.Category.TABLE_DROP_WITH_IF_EXISTS_AFTER_TABLE;
import static org.hibernate.orm.test.schemafilter.RecordingTarget.Category.TABLE_DROP_WITH_IF_EXISTS_BEFORE_TABLE;


@SuppressWarnings({ "rawtypes", "unchecked" })
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportCatalogCreation.class)
public class CatalogFilterTest extends BaseSchemaUnitTestCase {

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
		return new Class[] {
				CatalogNoneEntity0.class,
				Catalog1Entity1.class,
				Catalog1Entity2.class,
				Catalog2Entity3.class,
				Catalog2Entity4.class
		};
	}

	@SchemaTest
	public void createCatalog_unfiltered(SchemaScope schemaScope) {
		RecordingTarget target = doCreation( schemaScope, new DefaultSchemaFilter() );

		assertThat( target.getActions( TABLE_CREATE ), containsExactly(
				"the_entity_0",
				"the_catalog_1.the_entity_1",
				"the_catalog_1.the_entity_2",
				"the_catalog_2.the_entity_3",
				"the_catalog_2.the_entity_4"
		) );
	}

	@SchemaTest
	public void createCatalog_filtered(SchemaScope schemaScope) {
		RecordingTarget target = doCreation( schemaScope, new TestSchemaFilter() );

		assertThat(
				target.getActions( TABLE_CREATE ),
				containsExactly( "the_entity_0", "the_catalog_1.the_entity_1" )
		);
	}

	@SchemaTest
	public void dropCatalog_unfiltered(SchemaScope schemaScope) {
		RecordingTarget target = doDrop( schemaScope, new DefaultSchemaFilter() );
		assertThat( target.getTableDropAction( getDialect() ), containsExactly(
				"the_entity_0",
				"the_catalog_1.the_entity_1",
				"the_catalog_1.the_entity_2",
				"the_catalog_2.the_entity_3",
				"the_catalog_2.the_entity_4"
		) );
	}

	@SchemaTest
	public void dropCatalog_filtered(SchemaScope schemaScope) {
		RecordingTarget target = doDrop( schemaScope, new TestSchemaFilter() );

		assertThat(
				target.getTableDropAction( getDialect() ),
				containsExactly( "the_entity_0", "the_catalog_1.the_entity_1" )
		);
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

	private static class TestSchemaFilter implements SchemaFilter {

		@Override
		public boolean includeNamespace(Namespace namespace) {
			// exclude schema "the_catalog_2"
			Identifier identifier = namespace.getName().getCatalog();
			return identifier == null || !"the_catalog_2".equals( identifier.getText() );
		}

		@Override
		public boolean includeTable(ExportableTable table) {
			// exclude table "the_entity_2"
			return !"the_entity_2".equals( table.getTableName().getText() );
		}

		@Override
		public boolean includeSequence(Sequence sequence) {
			return true;
		}
	}

	@Entity
	@Table(name = "the_entity_1", catalog = "the_catalog_1")
	public static class Catalog1Entity1 {
		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "the_entity_2", catalog = "the_catalog_1")
	public static class Catalog1Entity2 {
		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "the_entity_3", catalog = "the_catalog_2")
	public static class Catalog2Entity3 {
		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "the_entity_4", catalog = "the_catalog_2")
	public static class Catalog2Entity4 {
		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}

	@Entity
	@Table(name = "the_entity_0")
	public static class CatalogNoneEntity0 {
		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}
}
