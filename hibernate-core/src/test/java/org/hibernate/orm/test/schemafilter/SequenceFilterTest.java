/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemafilter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.hibernate.tool.schema.internal.DefaultSchemaFilter;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;
import org.hibernate.tool.schema.internal.SchemaDropperImpl;
import org.hibernate.tool.schema.spi.SchemaFilter;

import org.hibernate.testing.orm.junit.JiraKey;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.cfg.JdbcSettings.FORMAT_SQL;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey(value = "HHH-10937")
@RequiresDialect(H2Dialect.class)
@ServiceRegistry(settings = @Setting(name=FORMAT_SQL, value = "false"))
@DomainModel(annotatedClasses = {
		SequenceFilterTest.Schema1Entity1.class,
		SequenceFilterTest.Schema2Entity2.class
})
public class SequenceFilterTest {
	@Test
	public void createSchema_unfiltered(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = doCreation( new DefaultSchemaFilter(), registryScope, modelScope );

		assertThat( target.getActions( RecordingTarget.Category.SEQUENCE_CREATE ), containsExactly(
				"entity_1_seq_gen",
				"entity_2_seq_gen"
		) );
	}

	@Test
	public void createSchema_filtered(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = doCreation( new TestSchemaFilter(), registryScope, modelScope );

		assertThat( target.getActions( RecordingTarget.Category.SEQUENCE_CREATE ), containsExactly(
				"entity_1_seq_gen"
		) );
	}

	@Test
	public void dropSchema_unfiltered(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = doDrop( new DefaultSchemaFilter(), registryScope, modelScope );

		assertThat( target.getActions( RecordingTarget.Category.SEQUENCE_DROP ), containsExactly(
				"entity_1_seq_gen",
				"entity_2_seq_gen"
		) );
	}

	@Test
	public void dropSchema_filtered(ServiceRegistryScope registryScope, DomainModelScope modelScope) {
		RecordingTarget target = doDrop( new TestSchemaFilter(), registryScope, modelScope );

		assertThat(
				target.getActions( RecordingTarget.Category.SEQUENCE_DROP ),
				containsExactly( "entity_1_seq_gen" )
		);
	}

	@Entity
	@SequenceGenerator(initialValue = 1, name = "idgen", sequenceName = "entity_1_seq_gen")
	@jakarta.persistence.Table(name = "the_entity_1", schema = "the_schema_1")
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
	@jakarta.persistence.Table(name = "the_entity_2", schema = "the_schema_2")
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
		public boolean includeTable(Table table) {
			return true;
		}

		@Override
		public boolean includeSequence(Sequence sequence) {
			return !"entity_2_seq_gen".endsWith( sequence.getName().render() );
		}
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

	private BaseMatcher<Set<String>> containsExactly(String... expected) {
		return containsExactly( new HashSet<>( Arrays.asList( expected ) ) );
	}

	private BaseMatcher<Set<String>> containsExactly(final Set<String> expected) {
		return new BaseMatcher<>() {
			@Override
			public boolean matches(Object item) {
				//noinspection unchecked
				var set = (Set<String>) item;
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
