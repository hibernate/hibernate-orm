/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.annotations.EmbeddableInstantiator;
import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.MapKeyJavaType;
import org.hibernate.annotations.MapKeyJdbcType;
import org.hibernate.annotations.MapKeyJdbcTypeCode;
import org.hibernate.annotations.MapKeyMutability;
import org.hibernate.annotations.MapKeyType;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdClass;
import org.hibernate.annotations.Nationalized;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.annotations.TimeZoneColumn;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.model.process.internal.EnumeratedValueConverter;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.metamodel.spi.ValueAccess;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.orm.test.idgen.GeneratorSettingsImpl;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.CustomType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumeratedValue;
import jakarta.persistence.EnumType;
import jakarta.persistence.ExcludedFromVersioning;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.boot.models.bind.BindingTestingHelper.checkDomainModel;

/**
 * @author Steve Ebersole
 */
public class AnnotationCoverageBindingTests {
	@Test
	@ServiceRegistry
	void testEntityAndBasicAnnotationCoverage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( CoverageEntity.class.getName() );
					final BasicValue tenant = (BasicValue) entityBinding.getProperty( "tenant" ).getValue();
					final BasicValue status = (BasicValue) entityBinding.getProperty( "status" ).getValue();
					final Component details = (Component) entityBinding.getProperty( "details" ).getValue();
					final org.hibernate.mapping.Collection codes = context.getMetadataCollector()
							.getCollectionBinding( CoverageEntity.class.getName() + ".codes" );

					assertThat( entityBinding.getTable().getRowId() ).isEqualTo( "ROWID" );
					assertThat( entityBinding.getTable().getChecks() )
							.extracting( org.hibernate.mapping.CheckConstraint::getName )
							.containsExactly( "ck_coverage_table" );
					assertThat( tenant.isPartitionKey() ).isTrue();
					assertThat( entityBinding.getProperty( "tenant" ).isOptimisticLocked() ).isFalse();
					assertThat( ( (org.hibernate.mapping.Column) tenant.getColumn() ).getCheckConstraints() )
							.extracting( org.hibernate.mapping.CheckConstraint::getConstraint )
							.containsExactly( "tenant_id <> ''" );
					assertThat( status.resolve().getValueConverter() )
							.isInstanceOf( EnumeratedValueConverter.class );
					final EnumeratedValueConverter<CoverageStatus, String> statusConverter =
							(EnumeratedValueConverter<CoverageStatus, String>) status.resolve().getValueConverter();
					assertThat( statusConverter.toRelationalValue( CoverageStatus.ACTIVE ) )
							.isEqualTo( "A" );
					assertThat( details.getParentProperty() ).isEqualTo( "owner" );
					assertThat( codes.getCollectionTable().getChecks() )
							.extracting( org.hibernate.mapping.CheckConstraint::getConstraint )
							.containsExactly( "code is not null" );
					assertThat( ( (org.hibernate.mapping.Column) ( (BasicValue) codes.getElement() ).getColumn() )
							.getCheckConstraints() )
							.extracting( org.hibernate.mapping.CheckConstraint::getName )
							.containsExactly( "ck_coverage_code_column" );
				},
				scope.getRegistry(),
				CoverageEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testEntityKnobAnnotationCoverage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( EntityKnobRoot.class.getName() );
					final PersistentClass subtypeBinding = context.getMetadataCollector()
							.getEntityBinding( EntityKnobSubtype.class.getName() );

					assertThat( entityBinding.useDynamicInsert() ).isTrue();
					assertThat( entityBinding.useDynamicUpdate() ).isTrue();
					assertThat( entityBinding.isConcreteProxy() ).isTrue();
					assertThat( subtypeBinding.isConcreteProxy() ).isTrue();
					assertThat( entityBinding.getNaturalIdClass() ).isNotNull();
					assertThat( entityBinding.getNaturalIdClass().getClassName() )
							.isEqualTo( EntityKnobNaturalId.class.getName() );
				},
				scope.getRegistry(),
				EntityKnobRoot.class,
				EntityKnobSubtype.class
		);
	}

	@Test
	@ServiceRegistry
	void testMemberShapingAnnotationCoverage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( MemberShapingEntity.class.getName() );
					final org.hibernate.mapping.Map keyedValues = (org.hibernate.mapping.Map) context.getMetadataCollector()
							.getCollectionBinding( MemberShapingEntity.class.getName() + ".keyedValues" );

					assertThat( entityBinding.getProperty( "lazyNotes" ).getLazyGroup() ).isEqualTo( "notes" );
					assertThat( entityBinding.getProperty( "keyedValues" ).getLazyGroup() ).isEqualTo( "values" );
					assertThat( ( (BasicValue) keyedValues.getIndex() ).getExplicitMutabilityPlanAccess() )
							.isNotNull();
					assertThat( ( (BasicValue) keyedValues.getIndex() ).getExplicitMutabilityPlanAccess()
							.apply( context.getMetadataCollector().getTypeConfiguration() ) )
							.isInstanceOf( MutableIntegerMutabilityPlan.class );
				},
				scope.getRegistry(),
				MemberShapingEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testBasicValueColumnAndTypeAnnotationCoverage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( BasicValueTypeCoverageEntity.class.getName() );
					final BasicValue javaTyped = (BasicValue) entityBinding.getProperty( "javaTyped" ).getValue();
					final BasicValue jdbcTyped = (BasicValue) entityBinding.getProperty( "jdbcTyped" ).getValue();
					final BasicValue jdbcCodeTyped = (BasicValue) entityBinding.getProperty( "jdbcCodeTyped" ).getValue();
					final BasicValue customTyped = (BasicValue) entityBinding.getProperty( "customTyped" ).getValue();
					final BasicValue mutableValue = (BasicValue) entityBinding.getProperty( "mutableValue" ).getValue();
					final BasicValue nationalized = (BasicValue) entityBinding.getProperty( "nationalized" ).getValue();
					final BasicValue zoned = (BasicValue) entityBinding.getProperty( "zoned" ).getValue();
					final Component composite = (Component) entityBinding.getProperty( "composite" ).getValue();
					final Component instantiated = (Component) entityBinding.getProperty( "instantiated" ).getValue();
					final org.hibernate.mapping.Map javaKeyed = (org.hibernate.mapping.Map) context.getMetadataCollector()
							.getCollectionBinding( BasicValueTypeCoverageEntity.class.getName() + ".javaKeyed" );
					final org.hibernate.mapping.Map jdbcKeyed = (org.hibernate.mapping.Map) context.getMetadataCollector()
							.getCollectionBinding( BasicValueTypeCoverageEntity.class.getName() + ".jdbcKeyed" );
					final org.hibernate.mapping.Map jdbcCodeKeyed = (org.hibernate.mapping.Map) context.getMetadataCollector()
							.getCollectionBinding( BasicValueTypeCoverageEntity.class.getName() + ".jdbcCodeKeyed" );
					final org.hibernate.mapping.Map customKeyed = (org.hibernate.mapping.Map) context.getMetadataCollector()
							.getCollectionBinding( BasicValueTypeCoverageEntity.class.getName() + ".customKeyed" );

					assertThat( column( entityBinding.getProperty( "transformed" ) ).getCustomRead() )
							.isEqualTo( "lower(transformed)" );
					assertThat( column( entityBinding.getProperty( "transformed" ) ).getCustomWrite() )
							.isEqualTo( "upper(?)" );

					assertThat( javaTyped.resolve().getDomainJavaType() ).isInstanceOf( LocalStringJavaType.class );
					assertThat( jdbcTyped.resolve().getJdbcType() ).isInstanceOf( LocalStringJdbcType.class );
					assertThat( jdbcCodeTyped.resolve().getJdbcType().getJdbcTypeCode() ).isEqualTo( SqlTypes.INTEGER );
					final CustomType<?> customType = (CustomType<?>) customTyped.resolve().getLegacyResolvedBasicType();
					assertThat( customType.getUserType() ).isInstanceOf( LocalStringUserType.class );
					assertThat( ( (LocalStringUserType) customType.getUserType() ).strategy ).isEqualTo( "basic" );
					assertThat( mutableValue.getExplicitMutabilityPlanAccess()
							.apply( context.getMetadataCollector().getTypeConfiguration() ) )
							.isInstanceOf( MutableIntegerMutabilityPlan.class );

					assertThat( nationalized.isNationalized() ).isTrue();
					assertThat( zoned.getTimeZoneStorageType() ).isEqualTo( TimeZoneStorageType.COLUMN );
					assertThat( column( entityBinding.getProperty( "zoned" ) ).getName() ).isEqualTo( "zoned_tz" );
					assertThat( entityBinding.getProperty( "zoned" ).isUpdatable() ).isFalse();

					assertThat( composite.getTypeName() ).isEqualTo( LocalCompositeUserType.class.getName() );
					assertThat( instantiated.getCustomInstantiator() ).isEqualTo( LocalInstantiator.class );

					assertThat( ( (BasicValue) javaKeyed.getIndex() ).resolve().getDomainJavaType() )
							.isInstanceOf( LocalStringJavaType.class );
					assertThat( ( (BasicValue) jdbcKeyed.getIndex() ).resolve().getJdbcType() )
							.isInstanceOf( LocalStringJdbcType.class );
					assertThat( ( (BasicValue) jdbcCodeKeyed.getIndex() ).resolve().getJdbcType().getJdbcTypeCode() )
							.isEqualTo( SqlTypes.INTEGER );
					final CustomType<?> mapKeyType = (CustomType<?>) ( (BasicValue) customKeyed.getIndex() )
							.resolve()
							.getLegacyResolvedBasicType();
					assertThat( mapKeyType.getUserType() ).isInstanceOf( LocalStringUserType.class );
					assertThat( ( (LocalStringUserType) mapKeyType.getUserType() ).strategy ).isEqualTo( "map-key" );
				},
				scope.getRegistry(),
				BasicValueTypeCoverageEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testStructuralMemberAnnotationCoverage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( StructuralMemberEntity.class.getName() );
					final Component home = (Component) entityBinding.getProperty( "home" ).getValue();
					final Component zip = (Component) home.getProperty( "zip" ).getValue();
					final Component targeted = (Component) entityBinding.getProperty( "targeted" ).getValue();
					final org.hibernate.mapping.Collection tags = context.getMetadataCollector()
							.getCollectionBinding( StructuralMemberEntity.class.getName() + ".tags" );
					final Component tagElement = (Component) tags.getElement();

					assertThat( home.getColumnNamingPattern() ).isEqualTo( "home_%s" );
					assertThat( column( home.getProperty( "lineOne" ) ).getName() ).isEqualTo( "home_line_one" );
					assertThat( zip.getColumnNamingPattern() ).isEqualTo( "zip_%s" );
					assertThat( column( zip.getProperty( "zipCode" ) ).getName() )
							.isEqualTo( "home_zip_zip_code" );
					assertThat( targeted.getComponentClassName() )
							.isEqualTo( StructuralTargetDetails.class.getName() );
					assertThat( column( targeted.getProperty( "targetName" ) ).getName() )
							.isEqualTo( "target_name" );
					assertThat( tagElement.getColumnNamingPattern() ).isEqualTo( "tag_%s" );
					assertThat( column( tagElement.getProperty( "label" ) ).getName() ).isEqualTo( "tag_label" );
				},
				scope.getRegistry(),
				StructuralMemberEntity.class
		);
	}

	@Test
	@ServiceRegistry
	void testGeneratedBasicAnnotationCoverage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( GeneratedCoverageEntity.class.getName() );
					final Component generatedDetails = (Component) entityBinding.getProperty( "generatedDetails" )
							.getValue();

					assertHasValueGenerator( entityBinding.getProperty( "createdAt" ) );
					assertHasValueGenerator( entityBinding.getProperty( "updatedAt" ) );
					assertHasValueGenerator( entityBinding.getProperty( "currentTimestamp" ) );
					assertHasValueGenerator( entityBinding.getProperty( "triggerGenerated" ) );
					assertHasValueGenerator( generatedDetails.getProperty( "embeddedCreatedAt" ) );
					assertHasValueGenerator( generatedDetails.getProperty( "embeddedComputed" ) );

					assertThat( column( entityBinding.getProperty( "status" ) ).getDefaultValue() ).isEqualTo( "'new'" );
					assertThat( column( entityBinding.getProperty( "collatedName" ) ).getCollation() )
							.isEqualTo( "ucs_basic" );
					assertThat( column( entityBinding.getProperty( "eventTime" ) ).getTemporalPrecision() )
							.isEqualTo( 3 );
					assertThat( column( generatedDetails.getProperty( "embeddedStatus" ) ).getDefaultValue() )
							.isEqualTo( "'embedded'" );
					assertThat( column( generatedDetails.getProperty( "embeddedCollatedName" ) ).getCollation() )
							.isEqualTo( "ucs_basic" );
					assertThat( column( generatedDetails.getProperty( "embeddedEventTime" ) ).getTemporalPrecision() )
							.isEqualTo( 6 );

					final Property computed = entityBinding.getProperty( "computed" );
					assertHasValueGenerator( computed );
					final BasicValue computedValue = (BasicValue) computed.getValue();
					final org.hibernate.mapping.Column computedColumn =
							(org.hibernate.mapping.Column) computedValue.getColumn();
					assertThat( computedColumn.getGeneratedAs() ).isEqualTo( "code || '-generated'" );

					final BasicValue embeddedComputedValue =
							(BasicValue) generatedDetails.getProperty( "embeddedComputed" ).getValue();
					final org.hibernate.mapping.Column embeddedComputedColumn =
							(org.hibernate.mapping.Column) embeddedComputedValue.getColumn();
					assertThat( embeddedComputedColumn.getGeneratedAs() ).isEqualTo( "embedded_code || '-generated'" );

					assertThat( entityBinding.getBatchSize() ).isEqualTo( 37 );
					assertThat( entityBinding.getProperty( "customBound" ).isUpdatable() ).isFalse();
					assertThat( entityBinding.getProperty( "version" ).isUpdatable() ).isFalse();
					assertThat( generatedDetails.isDynamic() ).isTrue();
					assertThat( generatedDetails.getProperty( "embeddedCustomBound" ).isUpdatable() ).isFalse();
				},
				scope.getRegistry(),
				GeneratedCoverageEntity.class
		);
	}

	private static void assertHasValueGenerator(Property property) {
		assertThat( property.getValueGeneratorCreator() )
				.as( property.getName() )
				.isNotNull();
	}

	private static org.hibernate.mapping.Column column(Property property) {
		return (org.hibernate.mapping.Column) ( (BasicValue) property.getValue() ).getColumn();
	}

	@Test
	@ServiceRegistry
	void testCoreMappingAnnotationCoverage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( CoreMappingRoot.class.getName() );
					final PersistentClass subtypeBinding = context.getMetadataCollector()
							.getEntityBinding( CoreMappingSubtype.class.getName() );
					final Join secondaryTable = entityBinding.getSecondaryTable( "core_mapping_details" );
					final Property name = entityBinding.getProperty( "name" );
					final Property notes = entityBinding.getProperty( "notes" );

					assertThat( entityBinding.getTable().getName() ).isEqualTo( "core_mapping_roots" );
					assertThat( secondaryTable ).isNotNull();
					assertThat( entityBinding.getDiscriminatorValue() ).isEqualTo( "ROOT" );
					assertThat( subtypeBinding.getDiscriminatorValue() ).isEqualTo( "SUB" );
					assertThat( entityBinding.getDiscriminator() ).isInstanceOf( BasicValue.class );
					assertThat( ( (org.hibernate.mapping.Column)
							( (BasicValue) entityBinding.getDiscriminator() ).getColumn() ).getName() )
							.isEqualTo( "kind" );
					assertThat( entityBinding.isVersioned() ).isTrue();
					assertThat( entityBinding.getVersion().getName() ).isEqualTo( "version" );
					assertThat( column( entityBinding.getVersion() ).getName() ).isEqualTo( "version" );

					assertThat( name.getPropertyAccessorName() ).isEqualTo( "field" );
					assertThat( name.isOptional() ).isFalse();
					assertThat( name.isLazy() ).isTrue();
					assertThat( column( name ).getName() ).isEqualTo( "name" );
					assertThat( column( name ).getLength() ).isEqualTo( 128 );
					assertThat( column( name ).isNullable() ).isFalse();

					assertThat( notes.isLazy() ).isTrue();
					assertThat( notes.isLob() ).isTrue();
					assertThat( ( (BasicValue) notes.getValue() ).isLob() ).isTrue();
					assertThat( notes.getValue().getTable() ).isSameAs( secondaryTable.getTable() );
					assertThat( column( notes ).getName() ).isEqualTo( "notes" );

					assertThat( entityBinding.getIdentifier().createGenerator(
							context.getMetadata().getDatabase().getDialect(),
							entityBinding,
							entityBinding.getIdentifierProperty(),
							new GeneratorSettingsImpl( context.getMetadata() )
					) ).isInstanceOf( SequenceStyleGenerator.class );
				},
				scope.getRegistry(),
				CoreMappingRoot.class,
				CoreMappingSubtype.class
		);
	}

	@Test
	@ServiceRegistry
	void testAssociationOnDeleteCoverage(ServiceRegistryScope scope) {
		checkDomainModel(
				(context) -> {
					final RootClass entityBinding = (RootClass) context.getMetadataCollector()
							.getEntityBinding( OnDeleteOwner.class.getName() );
					final ManyToOne parent = (ManyToOne) entityBinding.getProperty( "parent" ).getValue();
					final org.hibernate.mapping.Collection values = context.getMetadataCollector()
							.getCollectionBinding( OnDeleteOwner.class.getName() + ".values" );
					final org.hibernate.mapping.Collection targets = context.getMetadataCollector()
							.getCollectionBinding( OnDeleteOwner.class.getName() + ".targets" );
					final ManyToOne targetElement = (ManyToOne) targets.getElement();

					assertThat( parent.getOnDeleteAction() ).isEqualTo( OnDeleteAction.CASCADE );
					assertThat( ( (SimpleValue) values.getKey() ).getOnDeleteAction() ).isEqualTo( OnDeleteAction.CASCADE );
					assertThat( ( (SimpleValue) targets.getKey() ).getOnDeleteAction() ).isEqualTo( OnDeleteAction.CASCADE );
					assertThat( targetElement.getOnDeleteAction() ).isEqualTo( OnDeleteAction.CASCADE );
				},
				scope.getRegistry(),
				OnDeleteOwner.class,
				OnDeleteParent.class,
				OnDeleteTarget.class
		);
	}

	@Entity(name = "CoreMappingRoot")
	@Table(name = "core_mapping_roots")
	@SecondaryTable(name = "core_mapping_details")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "kind", discriminatorType = DiscriminatorType.STRING, length = 16)
	@DiscriminatorValue("ROOT")
	public static class CoreMappingRoot {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE)
		@Column(name = "id")
		private Long id;

		@Access(AccessType.FIELD)
		@Basic(optional = false, fetch = FetchType.LAZY)
		@Column(name = "name", nullable = false, length = 128)
		private String name;

		@Lob
		@Basic(fetch = FetchType.LAZY)
		@Column(name = "notes", table = "core_mapping_details")
		private String notes;

		@Version
		@Column(name = "version")
		private int version;
	}

	@Entity(name = "CoreMappingSubtype")
	@DiscriminatorValue("SUB")
	public static class CoreMappingSubtype extends CoreMappingRoot {
		@Column(name = "description")
		private String description;
	}

	@Entity(name = "CoverageEntity")
	@Table(
			name = "coverage_entities",
			check = @CheckConstraint(name = "ck_coverage_table", constraint = "tenant_id is not null")
	)
	@RowId("ROWID")
	public static class CoverageEntity {
		@Id
		@Column(name = "id")
		private Integer id;

		@PartitionKey
		@ExcludedFromVersioning
		@Column(name = "tenant_id", check = @CheckConstraint(constraint = "tenant_id <> ''"))
		private String tenant;

		@Embedded
		private CoverageDetails details;

		@Enumerated(EnumType.STRING)
		@Column(name = "status_code")
		private CoverageStatus status;

		@ElementCollection
		@CollectionTable(
				name = "coverage_entity_codes",
				check = @CheckConstraint(name = "ck_coverage_codes", constraint = "code is not null")
		)
		@Column(name = "code", check = @CheckConstraint(name = "ck_coverage_code_column", constraint = "length(code) > 0"))
		private Set<String> codes;
	}

	public enum CoverageStatus {
		ACTIVE("A"),
		INACTIVE("I");

		@EnumeratedValue
		private final String code;

		CoverageStatus(String code) {
			this.code = code;
		}
	}

	@Embeddable
	public static class CoverageDetails {
		@Column(name = "name")
		private String name;

		@Parent
		private CoverageEntity owner;
	}

	@Entity(name = "EntityKnobRoot")
	@Table(name = "entity_knob_roots")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DynamicInsert
	@DynamicUpdate
	@ConcreteProxy
	@NaturalIdClass(EntityKnobNaturalId.class)
	public static class EntityKnobRoot {
		@Id
		@Column(name = "id")
		private Integer id;

		@NaturalId
		@Column(name = "tenant")
		private String tenant;

		@NaturalId
		@Column(name = "code")
		private String code;
	}

	@Entity(name = "EntityKnobSubtype")
	public static class EntityKnobSubtype extends EntityKnobRoot {
		@Column(name = "description")
		private String description;
	}

	public static class EntityKnobNaturalId {
		private String tenant;
		private String code;
	}

	@Entity(name = "MemberShapingEntity")
	@Table(name = "member_shaping_entities")
	public static class MemberShapingEntity {
		@Id
		@Column(name = "id")
		private Integer id;

		@LazyGroup("notes")
		@Column(name = "lazy_notes")
		private String lazyNotes;

		@ElementCollection
		@CollectionTable(name = "member_shaping_values")
		@MapKeyColumn(name = "map_key")
		@MapKeyMutability(MutableIntegerMutabilityPlan.class)
		@LazyGroup("values")
		private Map<Integer, String> keyedValues;
	}

	public static class MutableIntegerMutabilityPlan implements MutabilityPlan<Integer> {
		@Override
		public boolean isMutable() {
			return true;
		}

		@Override
		public Integer deepCopy(Integer value) {
			return value;
		}

		@Override
		public Serializable disassemble(Integer value, SharedSessionContract session) {
			return value;
		}

		@Override
		public Integer assemble(Serializable cached, SharedSessionContract session) {
			return (Integer) cached;
		}
	}

	@Entity(name = "BasicValueTypeCoverageEntity")
	@Table(name = "basic_value_type_coverage_entities")
	public static class BasicValueTypeCoverageEntity {
		@Id
		@Column(name = "id")
		private Integer id;

		@Column(name = "transformed")
		@ColumnTransformer(read = "lower(transformed)", write = "upper(?)")
		private String transformed;

		@Column(name = "java_typed")
		@JavaType(LocalStringJavaType.class)
		private String javaTyped;

		@Column(name = "jdbc_typed")
		@JdbcType(LocalStringJdbcType.class)
		private String jdbcTyped;

		@Column(name = "jdbc_code_typed")
		@JdbcTypeCode(SqlTypes.INTEGER)
		private String jdbcCodeTyped;

		@Column(name = "custom_typed")
		@Type(
				value = LocalStringUserType.class,
				parameters = @Parameter(name = "strategy", value = "basic")
		)
		private String customTyped;

		@Column(name = "mutable_value")
		@org.hibernate.annotations.Mutability(MutableIntegerMutabilityPlan.class)
		private Integer mutableValue;

		@Column(name = "nationalized")
		@Nationalized
		private String nationalized;

		@Column(name = "zoned")
		@TimeZoneStorage(TimeZoneStorageType.COLUMN)
		@TimeZoneColumn(name = "zoned_tz", updatable = false)
		private OffsetDateTime zoned;

		@Embedded
		@CompositeType(LocalCompositeUserType.class)
		private LocalCompositeDomain composite;

		@Embedded
		@EmbeddableInstantiator(LocalInstantiator.class)
		private LocalInstantiatedValue instantiated;

		@ElementCollection
		@CollectionTable(name = "basic_value_type_java_keyed")
		@MapKeyColumn(name = "map_key")
		@MapKeyJavaType(LocalStringJavaType.class)
		private Map<String, String> javaKeyed;

		@ElementCollection
		@CollectionTable(name = "basic_value_type_jdbc_keyed")
		@MapKeyColumn(name = "map_key")
		@MapKeyJdbcType(LocalStringJdbcType.class)
		private Map<String, String> jdbcKeyed;

		@ElementCollection
		@CollectionTable(name = "basic_value_type_jdbc_code_keyed")
		@MapKeyColumn(name = "map_key")
		@MapKeyJdbcTypeCode(SqlTypes.INTEGER)
		private Map<String, String> jdbcCodeKeyed;

		@ElementCollection
		@CollectionTable(name = "basic_value_type_custom_keyed")
		@MapKeyColumn(name = "map_key")
		@MapKeyType(
				value = LocalStringUserType.class,
				parameters = @Parameter(name = "strategy", value = "map-key")
		)
		private Map<String, String> customKeyed;
	}

	public static class LocalStringJavaType extends AbstractClassJavaType<String> implements BasicJavaType<String> {
		public LocalStringJavaType() {
			super( String.class );
		}

		@Override
		public org.hibernate.type.descriptor.jdbc.JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return indicators.getTypeConfiguration().getJdbcTypeRegistry().getDescriptor( SqlTypes.VARCHAR );
		}

		@Override
		public String fromString(CharSequence string) {
			return string == null ? null : string.toString();
		}

		@Override
		public <X> X unwrap(String value, Class<X> type, WrapperOptions options) {
			return type.isInstance( value ) ? type.cast( value ) : null;
		}

		@Override
		public <X> String wrap(X value, WrapperOptions options) {
			return value == null ? null : value.toString();
		}
	}

	public static class LocalStringJdbcType extends VarcharJdbcType {
	}

	public static class LocalStringUserType implements UserType<String>, ParameterizedType {
		private String strategy;

		@Override
		public void setParameterValues(Properties parameters) {
			strategy = parameters.getProperty( "strategy" );
		}

		@Override
		public int getSqlType() {
			return SqlTypes.VARCHAR;
		}

		@Override
		public Class<String> returnedClass() {
			return String.class;
		}

		@Override
		public String deepCopy(String value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}
	}

	public record LocalCompositeDomain(@Column(name = "composite_name") String name) {
	}

	@Embeddable
	public static class LocalCompositeEmbeddable {
		@Column(name = "composite_name")
		private String name;
	}

	public static class LocalCompositeUserType implements CompositeUserType<LocalCompositeDomain> {
		@Override
		public Object getPropertyValue(LocalCompositeDomain component, int property) throws org.hibernate.HibernateException {
			return component.name();
		}

		@Override
		public LocalCompositeDomain instantiate(ValueAccess values) {
			return new LocalCompositeDomain( values.getValue( 0, String.class ) );
		}

		@Override
		public Class<?> embeddable() {
			return LocalCompositeEmbeddable.class;
		}

		@Override
		public Class<LocalCompositeDomain> returnedClass() {
			return LocalCompositeDomain.class;
		}

		@Override
		public boolean equals(LocalCompositeDomain x, LocalCompositeDomain y) {
			return Objects.equals( x, y );
		}

		@Override
		public int hashCode(LocalCompositeDomain x) {
			return Objects.hashCode( x );
		}

		@Override
		public LocalCompositeDomain deepCopy(LocalCompositeDomain value) {
			return value;
		}

		@Override
		public boolean isMutable() {
			return false;
		}

		@Override
		public Serializable disassemble(LocalCompositeDomain value) {
			return value == null ? null : value.name();
		}

		@Override
		public LocalCompositeDomain assemble(Serializable cached, Object owner) {
			return cached == null ? null : new LocalCompositeDomain( (String) cached );
		}

		@Override
		public LocalCompositeDomain replace(LocalCompositeDomain detached, LocalCompositeDomain managed, Object owner) {
			return detached;
		}
	}

	@Embeddable
	public static class LocalInstantiatedValue {
		@Column(name = "instantiated_value")
		private String value;
	}

	public static class LocalInstantiator implements org.hibernate.metamodel.spi.EmbeddableInstantiator {
		@Override
		public Object instantiate(ValueAccess values) {
			final LocalInstantiatedValue result = new LocalInstantiatedValue();
			result.value = values.getValue( 0, String.class );
			return result;
		}

		@Override
		public boolean isInstance(Object object) {
			return object instanceof LocalInstantiatedValue;
		}

		@Override
		public boolean isSameClass(Object object) {
			return object != null && object.getClass().equals( LocalInstantiatedValue.class );
		}
	}

	@Entity(name = "StructuralMemberEntity")
	@Table(name = "structural_member_entities")
	public static class StructuralMemberEntity {
		@Id
		@Column(name = "id")
		private Integer id;

		@Embedded
		@EmbeddedColumnNaming("home_%s")
		private StructuralAddress home;

		@Embedded
		@TargetEmbeddable(StructuralTargetDetails.class)
		private StructuralTargetBase targeted;

		@ElementCollection
		@CollectionTable(name = "structural_tags")
		@EmbeddedColumnNaming("tag_%s")
		private Set<StructuralTag> tags;
	}

	@Embeddable
	public static class StructuralAddress {
		@Column(name = "line_one")
		private String lineOne;

		@Embedded
		@EmbeddedColumnNaming("zip_%s")
		private StructuralZip zip;
	}

	@Embeddable
	public static class StructuralZip {
		@Column(name = "zip_code")
		private String zipCode;
	}

	public abstract static class StructuralTargetBase {
	}

	@Embeddable
	public static class StructuralTargetDetails extends StructuralTargetBase {
		@Column(name = "target_name")
		private String targetName;
	}

	@Embeddable
	public static class StructuralTag {
		@Column(name = "label")
		private String label;
	}

	@Entity(name = "GeneratedCoverageEntity")
	@Table(name = "generated_coverage_entities")
	@CustomEntityBinding
	public static class GeneratedCoverageEntity {
		@Id
		@Column(name = "id")
		private Integer id;

		@Version
		@Column(name = "version")
		@CustomAttributeBinding
		private int version;

		@Column(name = "created_at")
		@CreationTimestamp(source = SourceType.VM)
		private Instant createdAt;

		@Column(name = "updated_at")
		@UpdateTimestamp(source = SourceType.VM)
		private Instant updatedAt;

		@Column(name = "current_timestamp")
		@CurrentTimestamp(source = SourceType.VM)
		private Instant currentTimestamp;

		@Column(name = "trigger_generated")
		@Generated
		private String triggerGenerated;

		@Column(name = "computed")
		@GeneratedColumn("code || '-generated'")
		private String computed;

		@Column(name = "status")
		@ColumnDefault("'new'")
		private String status;

		@Column(name = "collated_name")
		@Collate("ucs_basic")
		private String collatedName;

		@SuppressWarnings({"deprecation", "removal"})
		@Column(name = "event_time")
		@FractionalSeconds(3)
		private LocalTime eventTime;

		@Column(name = "custom_bound")
		@CustomAttributeBinding
		private String customBound;

		@Embedded
		private GeneratedCoverageDetails generatedDetails;
	}

	@Embeddable
	@CustomComponentBinding
	public static class GeneratedCoverageDetails {
		@Column(name = "embedded_created_at")
		@CreationTimestamp(source = SourceType.VM)
		private Instant embeddedCreatedAt;

		@Column(name = "embedded_computed")
		@GeneratedColumn("embedded_code || '-generated'")
		private String embeddedComputed;

		@Column(name = "embedded_status")
		@ColumnDefault("'embedded'")
		private String embeddedStatus;

		@Column(name = "embedded_collated_name")
		@Collate("ucs_basic")
		private String embeddedCollatedName;

		@SuppressWarnings({"deprecation", "removal"})
		@Column(name = "embedded_event_time")
		@FractionalSeconds(6)
		private LocalTime embeddedEventTime;

		@Column(name = "embedded_custom_bound")
		@CustomAttributeBinding
		private String embeddedCustomBound;
	}

	@Target({ FIELD, METHOD })
	@Retention(RUNTIME)
	@AttributeBinderType(binder = CustomAttributeBinding.Binder.class)
	public @interface CustomAttributeBinding {
		class Binder implements AttributeBinder<CustomAttributeBinding> {
			@Override
			public void bind(
					CustomAttributeBinding annotation,
					MetadataBuildingContext buildingContext,
					PersistentClass persistentClass,
					Property property) {
				property.setUpdatable( false );
			}
		}
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	@TypeBinderType(binder = CustomEntityBinding.Binder.class)
	public @interface CustomEntityBinding {
		class Binder implements TypeBinder<CustomEntityBinding> {
			@Override
			public void bind(
					CustomEntityBinding annotation,
					MetadataBuildingContext buildingContext,
					PersistentClass persistentClass) {
				persistentClass.setBatchSize( 37 );
			}

			@Override
			public void bind(
					CustomEntityBinding annotation,
					MetadataBuildingContext buildingContext,
					Component embeddableClass) {
				throw new AssertionError( "Should not be called for embeddables" );
			}
		}
	}

	@Target(TYPE)
	@Retention(RUNTIME)
	@TypeBinderType(binder = CustomComponentBinding.Binder.class)
	public @interface CustomComponentBinding {
		class Binder implements TypeBinder<CustomComponentBinding> {
			@Override
			public void bind(
					CustomComponentBinding annotation,
					MetadataBuildingContext buildingContext,
					PersistentClass persistentClass) {
				throw new AssertionError( "Should not be called for entities" );
			}

			@Override
			public void bind(
					CustomComponentBinding annotation,
					MetadataBuildingContext buildingContext,
					Component embeddableClass) {
				embeddableClass.setDynamic( true );
			}
		}
	}

	@Entity(name = "OnDeleteOwner")
	@Table(name = "on_delete_owners")
	public static class OnDeleteOwner {
		@Id
		@Column(name = "id")
		private Integer id;

		@jakarta.persistence.ManyToOne
		@JoinColumn(name = "parent_id")
		@OnDelete(action = OnDeleteAction.CASCADE)
		private OnDeleteParent parent;

		@ElementCollection
		@CollectionTable(name = "on_delete_values", joinColumns = @JoinColumn(name = "owner_id"))
		@OnDelete(action = OnDeleteAction.CASCADE)
		private Set<String> values;

		@ManyToMany
		@JoinTable(
				name = "on_delete_owner_targets",
				joinColumns = @JoinColumn(name = "owner_id"),
				inverseJoinColumns = @JoinColumn(name = "target_id")
		)
		@OnDelete(action = OnDeleteAction.CASCADE)
		private Set<OnDeleteTarget> targets;
	}

	@Entity(name = "OnDeleteParent")
	@Table(name = "on_delete_parents")
	public static class OnDeleteParent {
		@Id
		@Column(name = "id")
		private Integer id;
	}

	@Entity(name = "OnDeleteTarget")
	@Table(name = "on_delete_targets")
	public static class OnDeleteTarget {
		@Id
		@Column(name = "id")
		private Integer id;
	}
}
