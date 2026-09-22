/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.Struct;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.AggregateColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.serial.MetadataSerialization;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PhysicalTable;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Aggregate container naming and storage-scope regressions.
///
/// @author Steve Ebersole
@BaseUnitTest
class AggregateColumnNamingTest {
	@Test
	void physicalNamesAndContainerOverrides() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Direct.class, Flattened.class, Nested.class, Overridden.class, Address.class, Details.class ),
					new StandardImplicitNamingStrategy(), new Prefix() );
			final var direct = (Component) metadata.getEntityBinding( Direct.class.getName() ).getProperty( "address" ).getValue();
			assertThat( direct.getAggregateColumn().getName() ).isEqualTo( "p_address" );
			final var flat = (Component) metadata.getEntityBinding( Flattened.class.getName() ).getProperty( "details" ).getValue();
			assertThat( ((Component) flat.getProperty( "address" ).getValue()).getAggregateColumn().getName() ).isEqualTo( "p_address" );
			final var override = (Component) metadata.getEntityBinding( Overridden.class.getName() ).getProperty( "details" ).getValue();
			assertThat( ((Component) override.getProperty( "address" ).getValue()).getAggregateColumn().getName() ).isEqualTo( "p_shipping_address" );
			assertThat( metadata.getEntityBinding( Flattened.class.getName() ).getTable().getColumns() )
					.contains( ((Component) flat.getProperty( "address" ).getValue()).getAggregateColumn() );
			assertThat( metadata.getEntityBinding( Overridden.class.getName() ).getTable().getColumns() )
					.contains( ((Component) override.getProperty( "address" ).getValue()).getAggregateColumn() );
			final var table = (PhysicalTable) metadata.getEntityBinding( Overridden.class.getName() ).getTable();
			assertThat( table.getIndexes().get( "shipping_lookup" ).getSelectables().get( 0 ) )
					.isEqualTo( ((Component) override.getProperty( "address" ).getValue()).getAggregateColumn() );
			final var nested = (Component) metadata.getEntityBinding( Nested.class.getName() ).getProperty( "details" ).getValue();
			assertThat( nested.getAggregateColumn().getName() ).isEqualTo( "p_details" );
			assertThat( metadata.getEntityBinding( Nested.class.getName() ).getTable().getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactlyInAnyOrder( "p_id", "p_details" );
			assertThat( ((Component) nested.getProperty( "address" ).getValue()).getAggregateColumn().getName() ).isEqualTo( "p_address" );
		}
	}

	@ParameterizedTest
	@MethodSource("org.hibernate.orm.test.namingstrategy.ImplicitNamingStrategyMatrixTest#cases")
	void defaultNamesAcrossStrategies(ImplicitNamingStrategyMatrixTest.Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Direct.class, Flattened.class, Address.class, Details.class ),
					test.strategy().implementation, new ImplicitNamingStrategyMatrixTest.PhysicalStrategy( test.prefix() ) );
			final var direct = (Component) metadata.getEntityBinding( Direct.class.getName() ).getProperty( "address" ).getValue();
			final var flat = (Component) metadata.getEntityBinding( Flattened.class.getName() ).getProperty( "details" ).getValue();
			assertThat( direct.getAggregateColumn().getName() ).isEqualTo( test.physical( "address" ) );
			assertThat( ((Component) flat.getProperty( "address" ).getValue()).getAggregateColumn().getName() ).isEqualTo( test.physical( "address" ) );
		}
	}

	@Test
	void inputScopesAndExplicitBypass() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var naming = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Direct.class, Flattened.class, Nested.class, Deep.class,
							Overridden.class, Explicit.class, Address.class, Details.class, Envelope.class ), naming, physical );
			assertThat( naming.inputs ).hasSize( 6 );
			assertThat( naming.inputs ).filteredOn( i -> i.entity().getClassName().equals( Overridden.class.getName() )
					|| i.entity().getClassName().equals( Explicit.class.getName() ) ).isEmpty();
			assertThat( naming.inputs ).filteredOn( i -> i.entity().getClassName().equals( Nested.class.getName() ) && i.attributePath().equals( "details.address" ) )
					.singleElement().satisfies( i -> assertThat( i.scope() ).isEqualTo( AggregateColumnNamingInput.Scope.AGGREGATE_MEMBER ) );
			assertThat( naming.inputs ).filteredOn( i -> i.entity().getClassName().equals( Flattened.class.getName() ) )
					.singleElement().satisfies( i -> {
						assertThat( i.attributePath() ).isEqualTo( "details.address" );
						assertThat( i.scope() ).isEqualTo( AggregateColumnNamingInput.Scope.TABLE_COLUMN );
						assertThat( i.embeddableTypeName() ).isEqualTo( Address.class.getName() );
					} );
			assertThat( naming.inputs ).allSatisfy( i -> {
				assertThat( i.storageKind() ).isEqualTo( AggregateColumnNamingInput.StorageKind.JSON );
				assertThat( i.usage() ).isEqualTo( AggregateColumnNamingInput.Usage.ATTRIBUTE );
				assertThat( i.plural() ).isFalse();
			} );
			assertThat( physical.inputs ).filteredOn( n -> n.getText().startsWith( "agg_" ) ).hasSize( 6 );
			final var deep = metadata.getEntityBinding( Deep.class.getName() );
			assertThat( deep.getTable().getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( "p_id", "p_agg_envelope" );
			final var explicit = (Component) metadata.getEntityBinding( Explicit.class.getName() ).getProperty( "address" ).getValue();
			assertThat( explicit.getAggregateColumn().getName() ).isEqualTo( "p_chosen" );
			assertThat( explicit.getAggregateColumn().isQuoted() ).isTrue();
		}
	}

	@Test
	void repeatedMemberNamesRemainInSeparateAggregateContainers() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var naming = new Recording();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Pair.class, Address.class, Details.class ), naming, new Prefix() );
			final var entity = metadata.getEntityBinding( Pair.class.getName() );
			assertThat( entity.getTable().getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( "p_id", "p_agg_first", "p_agg_second" );
			final var first = (Component) entity.getProperty( "first" ).getValue();
			final var second = (Component) entity.getProperty( "second" ).getValue();
			final var firstMember = ((Component) first.getProperty( "address" ).getValue()).getAggregateColumn();
			final var secondMember = ((Component) second.getProperty( "address" ).getValue()).getAggregateColumn();
			assertThat( firstMember ).isNotSameAs( secondMember );
			assertThat( firstMember.getName() ).isEqualTo( "p_agg_address" ).isEqualTo( secondMember.getName() );
			assertThat( naming.inputs ).hasSize( 4 );
		}
	}

	@Test
	void pluralAndMapKeyInputs() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var naming = new Recording();
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Plural.class, Address.class ), naming, new Prefix() );
			assertThat( naming.inputs ).hasSize( 3 );
			assertThat( naming.inputs ).filteredOn( i -> i.usage() == AggregateColumnNamingInput.Usage.MAP_KEY ).singleElement()
					.satisfies( i -> {
						assertThat( i.attributePath() ).isEqualTo( "keyed" );
						assertThat( i.attributeName() ).isEqualTo( "keyed" );
						assertThat( i.plural() ).isFalse();
					} );
			assertThat( naming.inputs ).filteredOn( i -> i.usage() == AggregateColumnNamingInput.Usage.COLLECTION_ELEMENT ).singleElement()
					.satisfies( i -> assertThat( i.attributePath() ).isEqualTo( "elements" ) );
			assertThat( naming.inputs ).filteredOn( i -> i.usage() == AggregateColumnNamingInput.Usage.ATTRIBUTE ).singleElement()
					.satisfies( i -> {
						assertThat( i.attributePath() ).isEqualTo( "array" );
						assertThat( i.plural() ).isTrue();
					} );
		}
	}

	@Test
	void emptyOverrideReplacesDirectName() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var naming = new Recording();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( EmptyOverride.class, NamedDetails.class, Address.class ), naming, new Prefix() );
			assertThat( naming.inputs ).singleElement().satisfies( i -> assertThat( i.attributePath() ).isEqualTo( "details.address" ) );
			final var details = (Component) metadata.getEntityBinding( EmptyOverride.class.getName() ).getProperty( "details" ).getValue();
			final var column = ((Component) details.getProperty( "address" ).getValue()).getAggregateColumn();
			assertThat( column.getName() ).isEqualTo( "p_agg_address" );
			assertThat( column.getLength() ).isEqualTo( 512 );
			assertThat( column.isNullable() ).isFalse();
		}
	}

	@Test
	void xmlAndStructStorageInputs() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.dialect", PostgreSQLDialect.class ).build()) {
			final var naming = new Recording();
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( OtherStorage.class, Address.class, StructAddress.class ), naming, new Prefix() );
			assertThat( naming.inputs ).extracting( AggregateColumnNamingInput::storageKind )
					.containsExactlyInAnyOrder( AggregateColumnNamingInput.StorageKind.XML, AggregateColumnNamingInput.StorageKind.STRUCT );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void invalidResults(boolean returnNull) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithImplicitNaming( registry,
					new MappingSources().addManagedClasses( Direct.class, Address.class ), new StandardImplicitNamingStrategy() {
						@Override
						public LogicalName determineAggregateColumnName(AggregateColumnNamingInput input, ImplicitNamingContext context) {
							return returnNull ? null : new LogicalName( "invalid", false, true );
						}
					} ) ).hasMessageContaining( "non-null implicit name for aggregate column" );
		}
	}

	@Test
	void transformedNestedNamesWorkAtRuntime() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "jakarta.persistence.schema-generation.database.action", "create-drop" ).build()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Nested.class, Deep.class, Address.class, Details.class, Envelope.class ),
					new Recording(), new Prefix() );
			try (var factory = SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() )) {
				try (var session = factory.openSession()) {
					final var tx = session.beginTransaction();
					final var nested = new Nested();
					nested.id = 1;
					nested.details = details();
					session.persist( nested );
					session.flush();

					final var deep = new Deep();
					deep.id = 3;
					deep.envelope = new Envelope();
					deep.envelope.details = details();
					session.persist( deep );
					session.flush();
					tx.commit();
				}
				try (var session = factory.openSession()) {
					assertThat( session.find( Nested.class, 1L ).details.address.street ).isEqualTo( "Main" );
					assertThat( session.find( Deep.class, 3L ).envelope.details.address.street ).isEqualTo( "Main" );
				}
			}
		}
	}

	@Test
	@FailureExpected(jiraKey = "HHH-20913", reason = "The aggregate is bound using its String member JDBC mapping")
	void flattenedAggregateNamesWorkAtRuntime() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "jakarta.persistence.schema-generation.database.action", "create-drop" ).build()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Flattened.class, Address.class, Details.class ),
					new Recording(), new Prefix() );
			try (var factory = SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() )) {
				try (var session = factory.openSession()) {
					final var tx = session.beginTransaction();
					final var entity = new Flattened();
					entity.id = 1;
					entity.details = details();
					session.persist( entity );
					tx.commit();
				}
				try (var session = factory.openSession()) {
					assertThat( session.find( Flattened.class, 1L ).details.address.street ).isEqualTo( "Main" );
				}
			}
		}
	}

	@Test
	void directOverridesAndExplicitMapKeys() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var naming = new Recording();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( RootOverride.class, ExplicitMap.class, Address.class ), naming, new Prefix() );
			assertThat( naming.inputs ).isEmpty();
			final var address = (Component) metadata.getEntityBinding( RootOverride.class.getName() ).getProperty( "address" ).getValue();
			assertThat( address.getAggregateColumn().getName() ).isEqualTo( "p_override_name" );
			final var map = (org.hibernate.mapping.Map) metadata.getCollectionBinding( ExplicitMap.class.getName() + ".keyed" );
			assertThat( ((Component) map.getIndex()).getAggregateColumn().getName() ).isEqualTo( "p_key_name" );
		}
	}

	@Test
	@FailureExpected(reason = "Aggregate value types are not restored by metadata archives; reproduced before naming migration")
	void archivesPreserveAggregateNamesWithoutNamingReplay() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( MappingSettings.METADATA_SERIALIZATION_ENABLED, true ).build()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Nested.class, Address.class, Details.class ), implicit, physical );
			final int implicitCalls = implicit.inputs.size();
			final int physicalCalls = physical.inputs.size();
			final var bytes = new ByteArrayOutputStream();
			MetadataSerialization.serialize( (MetadataImplementor) metadata ).writeTo( bytes );
			final var restored = MetadataSerialization.read(
					new ByteArrayInputStream( bytes.toByteArray() ) ).restore( registry ).getMetadata();
			final var outer = (Component) restored.getEntityBinding( Nested.class.getName() ).getProperty( "details" ).getValue();
			assertThat( outer.getAggregateColumn().getName() ).isEqualTo( "p_agg_details" );
			assertThat( outer.getAggregateColumn().isQuoted() ).isTrue();
			assertThat( ((Component) outer.getProperty( "address" ).getValue()).getAggregateColumn().getName() ).isEqualTo( "p_agg_address" );
			assertThat( implicit.inputs ).hasSize( implicitCalls );
			assertThat( physical.inputs ).hasSize( physicalCalls );
		}
	}

	private static Details details() {
		final var result = new Details();
		result.address = new Address();
		result.address.street = "Main";
		return result;
	}

	static class Recording extends StandardImplicitNamingStrategy {
		final List<AggregateColumnNamingInput> inputs = new ArrayList<>();
		@Override
		public LogicalName determineAggregateColumnName(AggregateColumnNamingInput input, ImplicitNamingContext context) {
			inputs.add( input );
			return context.implicitName( "agg_" + input.attributeName(), true );
		}
	}

	static class Prefix extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> inputs = new ArrayList<>();
		@Override
		public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
			inputs.add( name );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
		}
	}

	@Embeddable
	static class Address {
		String street;
	}

	@Embeddable
	static class Details {
		@JdbcTypeCode(SqlTypes.JSON)
		Address address;
	}

	@Entity
	static class Direct {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON)
		Address address;
	}

	@Entity
	static class Flattened {
		@Id
		long id;
		@Embedded
		Details details;
	}

	@Entity
	static class Pair {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON)
		Details first;
		@JdbcTypeCode(SqlTypes.JSON)
		Details second;
	}

	@Entity
	static class Nested {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON)
		Details details;
	}

	@Entity
	@Table(indexes = @Index(name = "shipping_lookup", columnList = "shipping_address"))
	static class Overridden {
		@Id
		long id;
		@Embedded
		@AttributeOverride(name = "address", column = @Column(name = "shipping_address"))
		Details details;
	}

	@Embeddable
	static class Envelope {
		@Embedded
		Details details;
	}

	@Entity
	static class Deep {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON)
		Envelope envelope;
	}

	@Entity
	static class Explicit {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "`chosen`")
		Address address;
	}

	@Entity
	@AttributeOverride(name = "address", column = @Column(name = "override_name"))
	static class RootOverride {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "direct_name")
		Address address;
	}

	@Entity
	static class ExplicitMap {
		@Id
		long id;
		@ElementCollection
		@MapKeyJdbcTypeCode(SqlTypes.JSON)
		@MapKeyColumn(name = "key_name")
		Map<Address, String> keyed;
	}

	@Entity
	static class Plural {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.JSON_ARRAY)
		Address[] array;
		@ElementCollection
		@JdbcTypeCode(SqlTypes.JSON)
		List<Address> elements;
		@ElementCollection
		@MapKeyJdbcTypeCode(SqlTypes.JSON)
		Map<Address, String> keyed;
	}

	@Embeddable
	static class NamedDetails {
		@JdbcTypeCode(SqlTypes.JSON)
		@Column(name = "direct_name")
		Address address;
	}

	@Entity
	static class EmptyOverride {
		@Id
		long id;
		@Embedded
		@AttributeOverride(name = "address", column = @Column(name = "", length = 512, nullable = false))
		NamedDetails details;
	}

	@Embeddable
	@Struct(name = "address_type")
	static class StructAddress {
		String street;
	}

	@Entity
	static class OtherStorage {
		@Id
		long id;
		@JdbcTypeCode(SqlTypes.SQLXML)
		Address xml;
		@Embedded
		StructAddress struct;
	}
}
