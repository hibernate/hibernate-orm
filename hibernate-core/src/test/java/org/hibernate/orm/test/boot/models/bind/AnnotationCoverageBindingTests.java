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
import java.util.Map;
import java.util.Set;

import org.hibernate.SharedSessionContract;
import org.hibernate.annotations.AttributeBinderType;
import org.hibernate.annotations.Collate;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.ConcreteProxy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.EmbeddedColumnNaming;
import org.hibernate.annotations.FractionalSeconds;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GeneratedColumn;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.annotations.MapKeyMutability;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NaturalIdClass;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Parent;
import org.hibernate.annotations.PartitionKey;
import org.hibernate.annotations.RowId;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.annotations.TypeBinderType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.binder.AttributeBinder;
import org.hibernate.binder.TypeBinder;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKeyColumn;
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
					final Component details = (Component) entityBinding.getProperty( "details" ).getValue();

					assertThat( entityBinding.getTable().getRowId() ).isEqualTo( "ROWID" );
					assertThat( tenant.isPartitionKey() ).isTrue();
					assertThat( details.getParentProperty() ).isEqualTo( "owner" );
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

	@Entity(name = "CoverageEntity")
	@Table(name = "coverage_entities")
	@RowId("ROWID")
	public static class CoverageEntity {
		@Id
		@Column(name = "id")
		private Integer id;

		@PartitionKey
		@Column(name = "tenant_id")
		private String tenant;

		@Embedded
		private CoverageDetails details;
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
