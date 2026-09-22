/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.boot.internal.SessionFactoryOptionsCollector;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.BasicColumnNamingInput;
import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;
import org.hibernate.boot.model.naming.spi.NamedTableNamingInput;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.model.naming.spi.TimeZoneColumnNamingInput;
import org.hibernate.boot.pipeline.internal.SessionFactoryPipeline;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.serial.MetadataSerialization;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.MappingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.mapping.Component;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.SecondaryTable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Defaults, dependency readiness, provenance, and execution for time-zone naming.
///
/// @author Steve Ebersole
@BaseUnitTest
class TimeZoneColumnNamingTest {
	@ParameterizedTest
	@MethodSource("org.hibernate.orm.test.namingstrategy.ImplicitNamingStrategyMatrixTest#cases")
	void suppliedDefaults(ImplicitNamingStrategyMatrixTest.Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Defaults.class, OverrideOwner.class ),
					test.strategy().implementation, new ImplicitNamingStrategyMatrixTest.PhysicalStrategy( test.prefix() ) );
			final var entity = metadata.getEntityBinding( Defaults.class.getName() );
			for (String property : List.of( "absent", "offsetTime", "zoned" )) {
				assertThat( entity.getProperty( property ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
						.containsExactlyInAnyOrder( test.physical( property ), test.physical( property + "_tz" ) );
			}
			final boolean fullPath = test.strategy() == ImplicitNamingStrategyMatrixTest.Strategy.STANDARD
					|| test.strategy() == ImplicitNamingStrategyMatrixTest.Strategy.COMPONENT_PATH;
			assertThat( entity.getProperty( "empty" ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( test.physical( "empty" ), test.physical( fullPath ? "empty_zoneOffset" : "zoneOffset" ) );
			for (String property : List.of( "namedBase", "emptyNamedBase" )) {
				final String base = property.equals( "namedBase" ) ? "event_time" : "other_time";
				assertThat( entity.getProperty( property ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
						.containsExactlyInAnyOrder( test.physical( base ), test.physical( property.equals( "namedBase" ) ? "event_time_tz" : fullPath ? "emptyNamedBase_zoneOffset" : "zoneOffset" ) );
			}
			final var overrides = metadata.getEntityBinding( OverrideOwner.class.getName() );
			assertThat( overrides.getProperty( "namedSource" ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( test.physical( "selected_time" ), test.physical( "source_time_tz" ) );
			assertThat( overrides.getProperty( "emptyOverride" ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( test.physical( "emptyOverride" ), test.physical( fullPath ? "emptyOverride_zoneOffset" : "zoneOffset" ) );
			assertThat( overrides.getProperty( "eventTime" ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( test.physical( "chosen_time" ), test.physical( "eventTime_tz" ) );
			assertThat( overrides.getProperty( "explicit" ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( test.physical( "explicit" ), test.physical( "chosen_offset" ) );
		}
	}

	@Test
	void suppliedCompanionDefaultsReuseSourceNamesAndCustomBasicNaming() {
		final List<String> decisions = new ArrayList<>();
		final var strategy = new StandardImplicitNamingStrategy() {
			@Override
			public LogicalName determineBasicColumnName(BasicColumnNamingInput input, ImplicitNamingContext context) {
				decisions.add( input.attributePath() );
				return context.implicitName( "basic_" + input.attributePath().replace( '.', '_' ), true );
			}
		};
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Defaults.class, OverrideOwner.class ), strategy, new Prefix() );
			assertThat( decisions ).containsExactlyInAnyOrder( "absent", "empty", "empty.zoneOffset",
					"offsetTime", "zoned", "emptyNamedBase.zoneOffset", "explicit", "eventTime",
					"emptyOverride", "emptyOverride.zoneOffset" );
			final var owner = metadata.getEntityBinding( OverrideOwner.class.getName() );
			assertThat( owner.getProperty( "eventTime" ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( "p_chosen_time", "p_basic_eventTime_tz" );
			final var defaults = metadata.getEntityBinding( Defaults.class.getName() );
			assertThat( defaults.getProperty( "absent" ).getColumns() )
					.allSatisfy( column -> assertThat( column.isQuoted() ).isTrue() )
					.extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( "p_basic_absent", "p_basic_absent_tz" );
		}
	}

	@Test
	void dependencyFactsAndExactlyOnceNaming() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Defaults.class, OverrideOwner.class, SecondaryOwner.class, InheritedOwner.class, Base.class ), implicit, physical );
			assertThat( implicit.basics ).containsExactlyInAnyOrder( "absent", "empty", "offsetTime", "zoned", "explicit", "eventTime", "inherited", "emptyOverride" );
			assertThat( implicit.inputs ).hasSize( 11 );
			assertThat( implicit.inputs ).allSatisfy( input -> {
				assertThat( input.attributePath() ).doesNotContain( "zoneOffset", "instant", "utcTime" );
				assertThat( input.temporalColumn().physicalName().getText() )
						.isEqualTo( "p_" + input.temporalColumn().logicalName().getText() );
			} );
			assertThat( implicit.inputs ).filteredOn( i -> i.owner().getClassName().equals( SecondaryOwner.class.getName() ) )
					.singleElement().satisfies( i -> {
						assertThat( ((NamedTableNamingInput) i.table()).names().logicalName().getText() ).isEqualTo( "extra" );
						assertThat( i.temporalColumn().logicalName().isExplicit() ).isFalse();
					} );
			assertThat( implicit.inputs ).filteredOn( i -> i.attributePath().equals( "inherited" ) ).singleElement()
					.satisfies( i -> assertThat( i.owner().getClassName() ).isEqualTo( InheritedOwner.class.getName() ) );
			assertThat( implicit.inputs ).filteredOn( i -> i.owner().getClassName().equals( OverrideOwner.class.getName() ) && i.attributePath().equals( "eventTime" ) ).singleElement()
					.satisfies( i -> {
						assertThat( i.temporalColumn().logicalName().getText() ).isEqualTo( "chosen_time" );
						assertThat( i.sourceColumnName() ).isEmpty();
						assertThat( i.companionDeclared() ).isFalse();
						assertThat( i.temporalColumn().logicalName().isExplicit() ).isTrue();
					} );
			assertThat( implicit.inputs ).filteredOn( i -> i.attributePath().equals( "namedSource" ) )
					.singleElement().satisfies( i -> assertThat( i.sourceColumnName() ).hasValueSatisfying( name -> {
						assertThat( name.getText() ).isEqualTo( "source_time" );
						assertThat( name.isExplicit() ).isTrue();
					} ) );
			assertThat( implicit.inputs ).filteredOn( i -> i.attributePath().equals( "emptyOverride" ) )
					.singleElement().satisfies( i -> assertThat( i.companionDeclared() ).isTrue() );
			assertThat( physical.inputs ).filteredOn( n -> n.getText().startsWith( "tz_" ) ).hasSize( 11 )
					.allSatisfy( n -> { assertThat( n.isExplicit() ).isFalse(); assertThat( n.isQuoted() ).isTrue(); } );
			assertThat( physical.inputs ).filteredOn( n -> n.getText().startsWith( "basic_" ) ).hasSize( 8 )
					.allSatisfy( n -> assertThat( n.isExplicit() ).isFalse() );
			assertThat( physical.inputs ).filteredOn( n -> n.getText().equals( "chosen_offset" ) ).singleElement()
					.satisfies( n -> assertThat( n.isExplicit() ).isTrue() );
			final var value = (Component) metadata.getEntityBinding( SecondaryOwner.class.getName() ).getProperty( "eventTime" ).getValue();
			final var offset = value.getProperty( "zoneOffset" ).getColumns().get( 0 );
			assertThat( offset.getComment() ).isEqualTo( "offset comment" );
			assertThat( offset.getOptions() ).isEqualTo( "offset_options" );
			assertThat( value.getProperty( "zoneOffset" ).isUpdatable() ).isFalse();
			assertThat( value.getColumnContainer().getColumn( offset.getPhysicalName() ) ).isSameAs( offset );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void storageSelectionAndSupportBoundaries(boolean nativeSupport) {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "hibernate.timezone.default_storage", "COLUMN" )
				.applySetting( "hibernate.dialect", nativeSupport ? H2Dialect.class : NoNative.class ).build()) {
			final var implicit = new Recording();
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Auto.class, Global.class, Normalized.class, Nested.class, Details.class, Elements.class, Keys.class ), implicit, new Prefix() );
			assertThat( implicit.inputs ).hasSize( nativeSupport ? 1 : 2 );
			assertThat( implicit.inputs ).extracting( i -> i.owner().getClassName() ).contains( Global.class.getName() );
			if ( nativeSupport ) {
				assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
						new MappingSources().addManagedClass( AutoColumn.class ), implicit, new Prefix() ) )
						.hasMessageContaining( "Illegal combination" );
			}
			else {
				MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
						new MappingSources().addManagedClass( AutoColumn.class ), implicit, new Prefix() );
				assertThat( implicit.inputs ).hasSize( 3 );
			}
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( SplitTables.class ), implicit, new Prefix() ) )
					.hasMessageContaining( "two different tables" );
		}
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	void invalidImplicitResult(boolean nullResult) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			assertThatThrownBy( () -> MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Defaults.class ), new StandardImplicitNamingStrategy() {
						@Override public LogicalName determineTimeZoneColumnName(TimeZoneColumnNamingInput input, ImplicitNamingContext context) {
							return nullResult ? null : new LogicalName( "invalid", false, true );
						}
					}, new Prefix() ) ).hasMessageContaining( "non-null implicit name for time-zone column" );
		}
	}

	@Test
	void quotedNamesWorkAtRuntimeAndSurviveArchives() {
		try (var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( MappingSettings.METADATA_SERIALIZATION_ENABLED, true )
				.applySetting( "jakarta.persistence.schema-generation.database.action", "create-drop" ).build()) {
			final var implicit = new Recording();
			final var physical = new Prefix();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Defaults.class ), implicit, physical );
			final var bytes = new ByteArrayOutputStream();
			MetadataSerialization.serialize( (MetadataImplementor) metadata ).writeTo( bytes );
			final var restored = MetadataSerialization.read( new ByteArrayInputStream( bytes.toByteArray() ) ).restore( registry ).getMetadata();
			assertThat( restored.getEntityBinding( Defaults.class.getName() ).getProperty( "absent" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getQuotedName ).containsExactlyInAnyOrder( "`p_basic_absent`", "`p_tz_basic_absent`" );
			assertThat( implicit.inputs ).hasSize( 6 );
			assertThat( implicit.basics ).hasSize( 4 );
			assertThat( physical.inputs ).hasSize( 13 );
			try (var factory = SessionFactoryPipeline.build( metadata, new SessionFactoryOptionsCollector() )) {
				final var dateTime = OffsetDateTime.parse( "2026-09-22T11:23:45+05:30" );
				try (var session = factory.openSession()) {
					final var tx = session.beginTransaction();
					final var entity = new Defaults();
					entity.id = 1;
					entity.absent = dateTime;
					entity.empty = dateTime;
					entity.namedBase = dateTime;
					entity.emptyNamedBase = dateTime;
					entity.offsetTime = dateTime.toOffsetTime();
					entity.zoned = dateTime.toZonedDateTime();
					session.persist( entity );
					tx.commit();
				}
				try (var session = factory.openSession()) {
					final var loaded = session.find( Defaults.class, 1L );
					assertThat( loaded.absent ).isEqualTo( dateTime );
					assertThat( loaded.empty ).isEqualTo( dateTime );
					assertThat( loaded.namedBase ).isEqualTo( dateTime );
					assertThat( loaded.emptyNamedBase ).isEqualTo( dateTime );
					assertThat( loaded.offsetTime ).isEqualTo( dateTime.toOffsetTime() );
					assertThat( loaded.zoned.toOffsetDateTime() ).isEqualTo( dateTime );
				}
			}
		}
	}

	@Test
	void patternsAndLogicalQuotingAreAppliedOnce() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var physicalInputs = new ArrayList<LogicalName>();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( QuotedPatterns.class ), new StandardImplicitNamingStrategy(), new Prefix() {
						@Override public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
							physicalInputs.add( name );
							return context.getPhysicalNameFactory().create( "p_" + name.getText(), true );
						}
					} );
			final var entity = metadata.getEntityBinding( QuotedPatterns.class.getName() );
			assertThat( entity.getProperty( "patterned" ).getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.containsExactlyInAnyOrder( "p_pattern_base", "p_pattern_base_tz" );
			assertThat( physicalInputs ).filteredOn( n -> n.getText().equals( "quoted_time_tz" ) ).singleElement()
					.satisfies( n -> { assertThat( n.isQuoted() ).isTrue(); assertThat( n.isExplicit() ).isFalse(); } );
			assertThat( physicalInputs ).filteredOn( n -> n.getText().equals( "physicalOnly_tz" ) ).singleElement()
					.satisfies( n -> assertThat( n.isQuoted() ).isFalse() );
		}
	}

	@Test
	void inheritedNamingIsPerConcreteEntityUse() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var recordedInputs = new ArrayList<TimeZoneColumnNamingInput>();
			final var strategy = new Recording() {
				@Override public LogicalName determineTimeZoneColumnName(TimeZoneColumnNamingInput input, ImplicitNamingContext context) {
					recordedInputs.add( input );
					return context.implicitName( input.owner().getClassName().equals( InheritedOwner.class.getName() ) ? "first_tz" : "second_tz" );
				}
			};
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( InheritedOwner.class, SecondInheritedOwner.class, Base.class ), strategy, new Prefix() );
			assertThat( recordedInputs ).extracting( i -> i.owner().getClassName() )
					.containsExactlyInAnyOrder( InheritedOwner.class.getName(), SecondInheritedOwner.class.getName() );
			assertThat( strategy.basics ).containsExactly( "inherited", "inherited" );
			for (Class<?> type : List.of( InheritedOwner.class, SecondInheritedOwner.class )) {
				final var value = (Component) metadata.getEntityBinding( type.getName() ).getProperty( "inherited" ).getValue();
				assertThat( value.getProperty( "zoneOffset" ).getColumns().get( 0 ).getName() )
						.isEqualTo( type == InheritedOwner.class ? "p_first_tz" : "p_second_tz" );
			}
		}
	}

	@Test
	void validatesInputArguments() {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var naming = new Recording();
			MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Defaults.class ), naming, new Prefix() );
			final var input = naming.inputs.get( 0 );
			assertThatThrownBy( () -> new TimeZoneColumnNamingInput( input.owner(), input.attributePath(), input.temporalColumn(), input.table(), input.companionDeclared(), null ) )
					.isInstanceOf( NullPointerException.class );
			assertThatThrownBy( () -> new TimeZoneColumnNamingInput( null, input.attributePath(), input.temporalColumn(), input.table(), input.companionDeclared(), input.sourceColumnName() ) )
					.isInstanceOf( NullPointerException.class );
			assertThatThrownBy( () -> new TimeZoneColumnNamingInput( input.owner(), null, input.temporalColumn(), input.table(), input.companionDeclared(), input.sourceColumnName() ) )
					.isInstanceOf( NullPointerException.class );
			assertThatThrownBy( () -> new TimeZoneColumnNamingInput( input.owner(), "", input.temporalColumn(), input.table(), input.companionDeclared(), input.sourceColumnName() ) )
					.isInstanceOf( IllegalArgumentException.class );
			assertThatThrownBy( () -> new TimeZoneColumnNamingInput( input.owner(), input.attributePath(), null, input.table(), input.companionDeclared(), input.sourceColumnName() ) )
					.isInstanceOf( NullPointerException.class );
			assertThatThrownBy( () -> new TimeZoneColumnNamingInput( input.owner(), input.attributePath(), input.temporalColumn(), null, input.companionDeclared(), input.sourceColumnName() ) )
					.isInstanceOf( NullPointerException.class );
		}
	}

	@Entity static class QuotedPatterns {
		@Id long id;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @Column(name="`quoted_time`") OffsetDateTime quoted;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) OffsetDateTime physicalOnly;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @Column(name="base")
		@org.hibernate.annotations.EmbeddedColumnNaming("pattern_%s") OffsetDateTime patterned;
	}

	static class Recording extends StandardImplicitNamingStrategy {
		final List<String> basics = new ArrayList<>();
		final List<TimeZoneColumnNamingInput> inputs = new ArrayList<>();
		@Override public LogicalName determineBasicColumnName(BasicColumnNamingInput input, ImplicitNamingContext context) {
			basics.add( input.attributePath() );
			return context.implicitName( "basic_" + input.attributePath().replace( '.', '_' ), true );
		}
		@Override public LogicalName determineTimeZoneColumnName(TimeZoneColumnNamingInput input, ImplicitNamingContext context) {
			inputs.add( input );
			return context.implicitName( "tz_" + input.temporalColumn().logicalName().getText(), true );
		}
	}
	static class Prefix extends PhysicalNamingStrategyStandardImpl {
		final List<LogicalName> inputs = new ArrayList<>();
		@Override public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
			inputs.add( name );
			return context.getPhysicalNameFactory().create( "p_" + name.getText(), false );
		}
	}
	public static class NoNative extends H2Dialect {
		@Override public TimeZoneSupport getTimeZoneSupport() { return TimeZoneSupport.NONE; }
	}

	@Entity static class Defaults {
		@Id long id;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) OffsetDateTime absent;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @TimeZoneColumn OffsetDateTime empty;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @Column(name="event_time") OffsetDateTime namedBase;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @Column(name="other_time") @TimeZoneColumn OffsetDateTime emptyNamedBase;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) OffsetTime offsetTime;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) ZonedDateTime zoned;
	}
	@Entity static class OverrideOwner {
		@Id long id;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @AttributeOverride(name="instant",column=@Column(name="chosen_time")) OffsetDateTime eventTime;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @Column(name="source_time")
		@AttributeOverride(name="instant",column=@Column(name="selected_time")) OffsetDateTime namedSource;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @TimeZoneColumn(name="ignored")
		@AttributeOverride(name="zoneOffset",column=@Column) OffsetDateTime emptyOverride;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @TimeZoneColumn(name="ignored_offset")
		@AttributeOverride(name="zoneOffset",column=@Column(name="chosen_offset")) OffsetDateTime explicit;
	}
	@Entity @SecondaryTable(name="extra") static class SecondaryOwner {
		@Id long id;
		@TimeZoneStorage(TimeZoneStorageType.COLUMN) @Column(table="extra")
		@TimeZoneColumn(table="extra",comment="offset comment",options="offset_options",updatable=false) OffsetDateTime eventTime;
	}
	@MappedSuperclass static class Base { @TimeZoneStorage(TimeZoneStorageType.COLUMN) OffsetDateTime inherited; }
	@Entity static class InheritedOwner extends Base { @Id long id; }
	@Entity static class SecondInheritedOwner extends Base { @Id long id; }
	@Entity static class Auto { @Id long id; @TimeZoneStorage(TimeZoneStorageType.AUTO) OffsetDateTime eventTime; }
	@Entity static class AutoColumn { @Id long id; @TimeZoneStorage(TimeZoneStorageType.AUTO) @TimeZoneColumn OffsetDateTime eventTime; }
	@Entity static class Global { @Id long id; OffsetDateTime eventTime; }
	@Entity static class Normalized { @Id long id; @TimeZoneStorage(TimeZoneStorageType.NORMALIZE_UTC) OffsetDateTime eventTime; }
	@Entity static class Nested { @Id long id; @Embedded Details details; }
	@Embeddable static class Details { @TimeZoneStorage(TimeZoneStorageType.COLUMN) OffsetDateTime eventTime; }
	@Entity static class Elements { @Id long id; @ElementCollection @TimeZoneStorage(TimeZoneStorageType.COLUMN) List<OffsetDateTime> events; }
	@Entity static class Keys { @Id long id; @ElementCollection Map<OffsetDateTime,String> events; }
	@Entity @SecondaryTable(name="extra") static class SplitTables {
		@Id long id; @TimeZoneStorage(TimeZoneStorageType.COLUMN) @TimeZoneColumn(table="extra") OffsetDateTime eventTime;
	}
}
