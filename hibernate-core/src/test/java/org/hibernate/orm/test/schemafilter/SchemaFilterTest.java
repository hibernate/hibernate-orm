/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemafilter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;

@JiraKey(value = "HHH-9876")
@SuppressWarnings({"rawtypes", "unchecked", "JUnitMalformedDeclaration"})
@RequiresDialect(value = SQLServerDialect.class, comment = "Unit test - limit to minimize complexity of checks")
@ServiceRegistry(settings = @Setting(name = FORMAT_SQL, value = "false"))
@DomainModel(annotatedClasses = {
		SchemaFilterTest.SchemaNoneEntity0.class,
		SchemaFilterTest.Schema1Entity1.class,
		SchemaFilterTest.Schema1Entity2.class,
		SchemaFilterTest.Schema2Entity3.class,
		SchemaFilterTest.Schema2Entity4.class
})
public class SchemaFilterTest {
	@Test
	public void createSchema_unfiltered(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = doCreation( new DefaultSchemaFilter(), registryScope, modelScope );

		assertThat( target.getActions( RecordingTarget.Category.SCHEMA_CREATE ), containsExactly(
				"the_schema_1",
				"the_schema_2"
		) );
		assertThat( target.getActions( RecordingTarget.Category.TABLE_CREATE ), containsExactly(
				"the_entity_0",
				"the_schema_1.the_entity_1",
				"the_schema_1.the_entity_2",
				"the_schema_2.the_entity_3",
				"the_schema_2.the_entity_4"
		) );
	}

	@Test
	public void createSchema_filtered(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = doCreation( new TestSchemaFilter(), registryScope, modelScope );

		assertThat( target.getActions( RecordingTarget.Category.SCHEMA_CREATE ), containsExactly(
				"the_schema_1"
		) );
		assertThat(
				target.getActions( RecordingTarget.Category.TABLE_CREATE ),
				containsExactly( "the_entity_0", "the_schema_1.the_entity_1" )
		);
	}

	@Test
	public void dropSchema_unfiltered(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = doDrop( new DefaultSchemaFilter(), registryScope, modelScope );

		assertThat( target.getActions( RecordingTarget.Category.SCHEMA_DROP ), containsExactly(
				"the_schema_1",
				"the_schema_2"
		) );
		assertThat( target.getActions( RecordingTarget.Category.TABLE_DROP ), containsExactly(
				"the_entity_0",
				"the_schema_1.the_entity_1",
				"the_schema_1.the_entity_2",
				"the_schema_2.the_entity_3",
				"the_schema_2.the_entity_4"
		) );
	}

	@Test
	public void dropSchema_filtered(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = doDrop( new TestSchemaFilter(), registryScope, modelScope );

		assertThat( target.getActions( RecordingTarget.Category.SCHEMA_DROP ), containsExactly(
				"the_schema_1"
		) );
		assertThat( target.getActions( RecordingTarget.Category.TABLE_DROP ), containsExactly(
				"the_entity_0",
				"the_schema_1.the_entity_1"
		) );
	}

	private RecordingTarget doCreation(SchemaFilter filter, ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = new RecordingTarget();
		new SchemaCreatorImpl( registryScope.getRegistry(), filter )
				.doCreation( modelScope.getDomainModel(), true, target );
		return target;
	}

	private RecordingTarget doDrop(SchemaFilter filter, ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = new RecordingTarget();
		new SchemaDropperImpl( registryScope.getRegistry(), filter )
				.doDrop( modelScope.getDomainModel(), true, target );
		return target;
	}

	private BaseMatcher<Set<String>> containsExactly(Object... expected) {
		return containsExactly( new HashSet( Arrays.asList( expected ) ) );
	}

	private BaseMatcher<Set<String>> containsExactly(final Set expected) {
		return new BaseMatcher<>() {
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
			// exclude schema "the_schema_2"
			Identifier identifier = namespace.getName().schema();
			if ( identifier != null ) {
				return !"the_schema_2".equals( identifier.getText() );
			}
			return true;
		}

		@Override
		public boolean includeTable(Table table) {
			// exclude table "the_entity_2"
			return !"the_entity_2".equals( table.getName() );
		}

		@Override
		public boolean includeSequence(Sequence sequence) {
			return true;
		}
	}

	@Entity
	@jakarta.persistence.Table(name = "the_entity_1", schema = "the_schema_1")
	public static class Schema1Entity1 {

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId( long id ) {
			this.id = id;
		}
	}

	@Entity
	@jakarta.persistence.Table(name = "the_entity_2", schema = "the_schema_1")
	public static class Schema1Entity2 {

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId( long id ) {
			this.id = id;
		}
	}

	@Entity
	@jakarta.persistence.Table(name = "the_entity_3", schema = "the_schema_2")
	public static class Schema2Entity3 {

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId( long id ) {
			this.id = id;
		}
	}

	@Entity
	@jakarta.persistence.Table(name = "the_entity_4", schema = "the_schema_2")
	public static class Schema2Entity4 {

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId( long id ) {
			this.id = id;
		}
	}

	@Entity
	@jakarta.persistence.Table(name = "the_entity_0")
	public static class SchemaNoneEntity0 {

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId( long id ) {
			this.id = id;
		}

	}
}
