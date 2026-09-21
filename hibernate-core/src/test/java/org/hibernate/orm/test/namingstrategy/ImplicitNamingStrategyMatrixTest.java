/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyComponentPathImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyJpaImpl;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.model.naming.spi.PhysicalNamingContext;
import org.hibernate.boot.model.naming.spi.StandardImplicitNamingStrategy;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.mapping.Component;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.relational.naming.spi.LogicalName;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Cross-strategy characterization of mapped names, independently specified expectations,
/// and physical transformation. This matrix is not a certification of JPA compliance.
///
/// @author Steve Ebersole
@BaseUnitTest
class ImplicitNamingStrategyMatrixTest {
	enum Strategy {
		STANDARD(new StandardImplicitNamingStrategy()),
		JPA(new ImplicitNamingStrategyJpaCompliantImpl()),
		LEGACY_JPA(new ImplicitNamingStrategyLegacyJpaImpl()),
		LEGACY_HBM(new ImplicitNamingStrategyLegacyHbmImpl()),
		COMPONENT_PATH(new ImplicitNamingStrategyComponentPathImpl());

		final ImplicitNamingStrategy implementation;
		Strategy(ImplicitNamingStrategy implementation) { this.implementation = implementation; }
	}

	record Case(Strategy strategy, String prefix) {
		String physical(String logical) { return prefix + logical; }
		boolean component() { return strategy == Strategy.COMPONENT_PATH || strategy == Strategy.STANDARD; }
		boolean hbm() { return strategy == Strategy.LEGACY_HBM; }
		@Override public String toString() { return strategy + (prefix.isEmpty() ? "/identity" : "/prefix"); }
	}

	static Stream<Case> cases() {
		return Stream.of( Strategy.values() ).flatMap( strategy -> Stream.of( new Case( strategy, "" ), new Case( strategy, "p_" ) ) );
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void independentColumns(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( ImplicitColumnNamingContractTest.Owner.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			final var owner = metadata.getEntityBinding( ImplicitColumnNamingContractTest.Owner.class.getName() );
			column( owner.getIdentifier().getColumns().get( 0 ), test, "id" );
			column( owner.getProperty( "title" ).getColumns().get( 0 ), test, "title" );
			column( owner.getProperty( "explicit" ).getColumns().get( 0 ), test, "explicit_column" );
			column( ((Component) owner.getProperty( "details" ).getValue()).getProperty( "zip" ).getColumns().get( 0 ),
					test, test.component() ? "details_zip" : "zip" );
			assertThat( owner.getProperty( "calculated" ).getValue().hasFormula() ).isTrue();
			final var values = (org.hibernate.mapping.List) metadata.getCollectionBinding( ImplicitColumnNamingContractTest.Owner.class.getName() + ".values" );
			column( values.getElement().getColumns().get( 0 ), test, "values" );
			column( values.getIndex().getColumns().get( 0 ), test, "values_ORDER" );
			final var labels = (org.hibernate.mapping.Map) metadata.getCollectionBinding( ImplicitColumnNamingContractTest.Owner.class.getName() + ".labels" );
			column( labels.getElement().getColumns().get( 0 ), test, "labels" );
			column( labels.getIndex().getColumns().get( 0 ), test, "labels_KEY" );
			final var explicit = (org.hibernate.mapping.List) metadata.getCollectionBinding( ImplicitColumnNamingContractTest.Owner.class.getName() + ".explicitValues" );
			column( explicit.getElement().getColumns().get( 0 ), test, "chosen_element" );
			column( explicit.getIndex().getColumns().get( 0 ), test, "chosen_order" );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void associationAndCollectionTables(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( MatrixOwner.class, MatrixTarget.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			final var owner = metadata.getEntityBinding( MatrixOwner.class.getName() );
			assertThat( owner.getTable().getName() ).isEqualTo( test.physical( "owner_table" ) );
			column( owner.getProperty( "target" ).getColumns().get( 0 ), test, test.hbm() ? "target" : "target_id" );
			column( owner.getProperty( "explicit" ).getColumns().get( 0 ), test, "chosen_fk" );
			assertThat( owner.getProperty( "explicit" ).getColumns().get( 0 ).isQuoted() ).isTrue();
			column( owner.getProperty( "version" ).getColumns().get( 0 ), test, "version" );
			final var targets = metadata.getCollectionBinding( MatrixOwner.class.getName() + ".targets" );
			final String associationTable = switch (test.strategy) {
				case STANDARD -> "owner_table_target_table";
				case LEGACY_HBM -> test.physical( "owner_table" ) + "_targets";
				default -> test.physical( "owner_table" ) + "_" + test.physical( "target_table" );
			};
			assertThat( targets.getCollectionTable().getName() ).isEqualTo( test.physical( associationTable ) );
			column( targets.getElement().getColumns().get( 0 ), test, test.hbm() ? "targets" : "targets_id" );
			final var tags = metadata.getCollectionBinding( MatrixOwner.class.getName() + ".tags" );
			final String entityName = test.hbm() ? "ImplicitNamingStrategyMatrixTest$MatrixOwner" : "OwnerAlias";
			final String collectionTable = test.strategy == Strategy.LEGACY_JPA ? test.physical( "owner_table" ) + "_tags" : entityName + "_tags";
			assertThat( tags.getCollectionTable().getName() ).isEqualTo( test.physical( collectionTable ) );
			final String ownerKey = (test.strategy == Strategy.LEGACY_JPA ? test.physical( "owner_table" ) : entityName) + "_id";
			column( tags.getKey().getColumns().get( 0 ), test, ownerKey );
			column( targets.getKey().getColumns().get( 0 ), test, ownerKey );
			column( tags.getElement().getColumns().get( 0 ), test, "tags" );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void secondaryTableAndEntityMapKeys(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( ImplicitJoinColumnNamingContractTest.Owner.class, ImplicitJoinColumnNamingContractTest.Target.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			final var owner = metadata.getEntityBinding( ImplicitJoinColumnNamingContractTest.Owner.class.getName() );
			column( owner.getJoins().get( 0 ).getKey().getColumns().get( 0 ), test, "id" );
			assertThat( owner.getJoins().get( 0 ).getTable().getName() ).isEqualTo( test.physical( "owner_details" ) );
			final var labels = (org.hibernate.mapping.Map) metadata.getCollectionBinding( ImplicitJoinColumnNamingContractTest.Owner.class.getName() + ".labels" );
			column( labels.getIndex().getColumns().get( 0 ), test, "labels_KEY" );
			column( owner.getProperty( "joinedTarget" ).getColumns().get( 0 ), test, test.hbm() ? "joinedTarget" : "joinedTarget_id" );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void nonPrimaryKeyAndInlineViewTargets(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( ImplicitJoinColumnNamingContractTest.BusinessOwner.class,
							ImplicitJoinColumnNamingContractTest.BusinessTarget.class,
							ImplicitJoinColumnNamingContractTest.ViewOwner.class, ImplicitJoinColumnNamingContractTest.ViewTarget.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			column( metadata.getEntityBinding( ImplicitJoinColumnNamingContractTest.BusinessOwner.class.getName() ).getProperty( "target" ).getColumns().get( 0 ),
					test, test.hbm() ? "target" : "target_business_code" );
			column( metadata.getEntityBinding( ImplicitJoinColumnNamingContractTest.ViewOwner.class.getName() ).getProperty( "target" ).getColumns().get( 0 ),
					test, test.hbm() ? "target" : "target_id" );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void anyAndTemporalCompanions(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( ImplicitColumnNamingContractTest.AnyOwner.class,
							ImplicitColumnNamingContractTest.Target.class, ImplicitColumnNamingContractTest.TemporalOwner.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			final var any = (org.hibernate.mapping.Any) metadata.getEntityBinding( ImplicitColumnNamingContractTest.AnyOwner.class.getName() ).getProperty( "target" ).getValue();
			column( any.getDiscriminatorDescriptor().getColumns().get( 0 ), test, "target_class" );
			column( any.getKeyDescriptor().getColumns().get( 0 ), test, "target_id" );
			assertThat( metadata.getEntityBinding( ImplicitColumnNamingContractTest.TemporalOwner.class.getName() ).getProperty( "eventTime" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactlyInAnyOrder( test.physical( "eventTime" ), test.physical( "eventTime_tz" ) );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void inheritance(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Base.class, Sub.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			final var sub = (org.hibernate.mapping.JoinedSubclass) metadata.getEntityBinding( Sub.class.getName() );
			column( sub.getKey().getColumns().get( 0 ), test, "id" );
			column( sub.getProperty( "detail" ).getColumns().get( 0 ), test, "detail" );
			assertThat( sub.getTable().getName() ).isEqualTo( test.physical( "sub_table" ) );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void explicitDerivedIdentity(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( DerivedColumnNamingTest.Parent.class,
							DerivedColumnNamingTest.ExplicitChild.class, DerivedColumnNamingTest.QuotedChild.class,
							DerivedColumnNamingTest.ReferringEntity.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			column( metadata.getEntityBinding( DerivedColumnNamingTest.ExplicitChild.class.getName() ).getIdentifier().getColumns().get( 0 ), test, "parent_fk" );
			final var quoted = metadata.getEntityBinding( DerivedColumnNamingTest.QuotedChild.class.getName() ).getIdentifier().getColumns().get( 0 );
			column( quoted, test, "QuotedFk" );
			assertThat( quoted.isQuoted() ).isTrue();
			column( metadata.getEntityBinding( DerivedColumnNamingTest.ReferringEntity.class.getName() ).getProperty( "child" ).getColumns().get( 0 ),
					test, test.hbm() ? "child" : "child_parent_fk" );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void explicitAndImplicitConstraints(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var physical = new PhysicalConstraintNamingTest.ConstraintStrategy();
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( PhysicalConstraintNamingTest.Owner.class, PhysicalConstraintNamingTest.Target.class ),
					test.strategy.implementation, test.prefix.isEmpty() ? new PhysicalNamingStrategyStandardImpl() : physical );
			final var table = (org.hibernate.mapping.PhysicalTable) metadata.getEntityBinding( PhysicalConstraintNamingTest.Owner.class.getName() ).getTable();
			assertThat( table.getForeignKeyCollection() ).hasSize( 2 ).extracting( org.hibernate.mapping.ForeignKey::getName )
					.contains( test.prefix.isEmpty() ? "owner_target" : "fk_owner_target" );
			assertThat( table.getIndexes().keySet() ).hasSize( 3 )
					.contains( test.prefix.isEmpty() ? "owner_code" : "ix_owner_code", test.prefix.isEmpty() ? "\"QuotedIndex\"" : "\"ix_QuotedIndex\"" );
			assertThat( table.getUniqueKeys().keySet() ).hasSize( 2 ).contains( test.prefix.isEmpty() ? "owner_code" : "uk_owner_code" );
			assertThat( table.getPrimaryKey().getName() ).isNotBlank();
			if ( !test.prefix.isEmpty() ) {
				assertThat( physical.foreignKeys ).hasSize( 2 ).anyMatch( LogicalName::isExplicit ).anyMatch( name -> !name.isExplicit() );
				assertThat( physical.indexes ).hasSize( 3 ).anyMatch( LogicalName::isExplicit ).anyMatch( name -> !name.isExplicit() );
				assertThat( physical.uniqueKeys ).hasSize( 2 ).anyMatch( LogicalName::isExplicit ).anyMatch( name -> !name.isExplicit() );
			}
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void compositeIdentifierWithExplicitJoinColumns(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( CompositeTarget.class, CompositeOwner.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			assertThat( metadata.getEntityBinding( CompositeOwner.class.getName() ).getProperty( "target" ).getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( test.physical( "fk_first" ), test.physical( "fk_second" ) );
			assertThat( metadata.getEntityBinding( CompositeTarget.class.getName() ).getIdentifier().getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( test.physical( "first_part" ), test.physical( "second_part" ) );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void repeatedNestedEmbeddables(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClass( Repeated.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			final var entity = metadata.getEntityBinding( Repeated.class.getName() );
			final var home = (Component) entity.getProperty( "home" ).getValue();
			final var work = (Component) entity.getProperty( "work" ).getValue();
			column( ((Component) home.getProperty( "location" ).getValue()).getProperty( "zip" ).getColumns().get( 0 ), test,
					test.component() ? "home_location_zip" : "zip" );
			column( ((Component) work.getProperty( "location" ).getValue()).getProperty( "zip" ).getColumns().get( 0 ), test,
					test.component() ? "work_location_zip" : "zip" );
			final var validated = (org.hibernate.boot.spi.MetadataImplementor) metadata;
			if ( test.component() ) {
				validated.validate();
			}
			else {
				assertThatThrownBy( validated::validate ).isInstanceOf( org.hibernate.MappingException.class ).hasMessageContaining( "zip" );
			}
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void inverseAndUnidirectionalRelationships(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Relations.class, Child.class, Detail.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			column( metadata.getEntityBinding( Child.class.getName() ).getProperty( "parent" ).getColumns().get( 0 ), test, test.hbm() ? "parent" : "parent_id" );
			final var children = metadata.getCollectionBinding( Relations.class.getName() + ".children" );
			assertThat( children.isInverse() ).isTrue();
			column( children.getKey().getColumns().get( 0 ), test, test.hbm() ? "parent" : "parent_id" );
			final var others = metadata.getCollectionBinding( Relations.class.getName() + ".others" );
			column( others.getKey().getColumns().get( 0 ), test, test.hbm() ? "others" : "others_id" );
			column( metadata.getEntityBinding( Relations.class.getName() ).getProperty( "detail" ).getColumns().get( 0 ), test, test.hbm() ? "detail" : "detail_id" );
		}
	}

	@ParameterizedTest(name = "{displayName}: {0}") @MethodSource("cases")
	void discriminatorAndTenant(Case test) {
		try (var registry = ServiceRegistryUtil.serviceRegistry()) {
			final var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( SingleBase.class, SingleSub.class ),
					test.strategy.implementation, new PhysicalStrategy( test.prefix ) );
			final var root = metadata.getEntityBinding( SingleBase.class.getName() );
			assertThat( root.getDiscriminator().getColumns().get( 0 ).getName() ).isEqualTo( test.prefix() + "DTYPE" );
			column( root.getProperty( "tenant" ).getColumns().get( 0 ), test, "tenant" );
			assertThat( metadata.getEntityBinding( SingleSub.class.getName() ).getTable() ).isSameAs( root.getTable() );
		}
	}

	private static void column(org.hibernate.mapping.Column column, Case test, String logical) {
		assertThat( column.getName() ).isEqualTo( test.physical( logical ) );
	}

	static class PhysicalStrategy extends PhysicalNamingStrategyStandardImpl {
		private final String prefix;
		PhysicalStrategy(String prefix) { this.prefix = prefix; }
		@Override public PhysicalName toPhysicalTableName(LogicalName name, PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( prefix + name.getText(), name.isQuoted() );
		}
		@Override public PhysicalName toPhysicalColumnName(LogicalName name, PhysicalNamingContext context) {
			return context.getPhysicalNameFactory().create( prefix + name.getText(), name.isQuoted() );
		}
	}

	@jakarta.persistence.Embeddable
	static class CompositeId {
		@jakarta.persistence.Column(name = "first_part") String first;
		@jakarta.persistence.Column(name = "second_part") String second;
	}
	@Entity @Table(name = "composite_target")
	static class CompositeTarget { @jakarta.persistence.EmbeddedId CompositeId id; }
	@Entity @Table(name = "composite_owner")
	static class CompositeOwner {
		@Id long id;
		@jakarta.persistence.ManyToOne
		@jakarta.persistence.JoinColumns({
				@jakarta.persistence.JoinColumn(name = "fk_second", referencedColumnName = "second_part"),
				@jakarta.persistence.JoinColumn(name = "fk_first", referencedColumnName = "first_part")})
		CompositeTarget target;
	}
	@jakarta.persistence.Embeddable
	static class Location { String zip; }
	@jakarta.persistence.Embeddable
	static class Address { @jakarta.persistence.Embedded Location location; }
	@Entity @Table(name = "repeated")
	static class Repeated {
		@Id long id;
		@jakarta.persistence.Embedded Address home;
		@jakarta.persistence.Embedded Address work;
	}

	@Entity @Table(name = "relations")
	static class Relations {
		@Id long id;
		@jakarta.persistence.OneToMany(mappedBy = "parent") java.util.Set<Child> children;
		@jakarta.persistence.OneToMany @jakarta.persistence.JoinColumn java.util.Set<Child> others;
		@jakarta.persistence.OneToOne Detail detail;
	}
	@Entity @Table(name = "child")
	static class Child { @Id long id; @jakarta.persistence.ManyToOne Relations parent; }
	@Entity @Table(name = "detail")
	static class Detail { @Id long id; @jakarta.persistence.OneToOne(mappedBy = "detail") Relations owner; }
	@Entity @Table(name = "single_base") @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	static class SingleBase { @Id long id; @org.hibernate.annotations.TenantId String tenant; }
	@Entity
	static class SingleSub extends SingleBase { String text; }

	@Entity(name = "OwnerAlias") @Table(name = "owner_table")
	static class MatrixOwner {
		@Id long id;
		@Version int version;
		@jakarta.persistence.ManyToOne MatrixTarget target;
		@jakarta.persistence.ManyToOne @jakarta.persistence.JoinColumn(name = "`chosen_fk`") MatrixTarget explicit;
		@jakarta.persistence.ManyToMany java.util.Set<MatrixTarget> targets;
		@jakarta.persistence.ElementCollection java.util.Set<String> tags;
	}
	@Entity(name = "TargetAlias") @Table(name = "target_table")
	static class MatrixTarget { @Id long id; }
	@Entity @Table(name = "base_table") @Inheritance(strategy = InheritanceType.JOINED)
	static class Base { @Id long id; }
	@Entity @Table(name = "sub_table")
	static class Sub extends Base { String detail; }
}
