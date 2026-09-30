/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.namingstrategy;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.SecondaryTable;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.pipeline.internal.source.MappingSources;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Value;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/// Common naming rules verified independently by both supported strategy suites.
/// Normative references use [Jakarta Persistence 4.0-M4]
/// (https://jakarta.ee/specifications/persistence/4.0/jakarta-persistence-spec-4.0-m4),
/// sections 2.4.2, 2.12, and 11.2, checked against the 4.0.0-M7 annotation sources.
///
/// @author Steve Ebersole
@BaseUnitTest
abstract class AbstractSupportedImplicitNamingTest {
	abstract ImplicitNamingStrategy strategy();

	void inspect(Consumer<Metadata> check, Class<?>... types) {
		inspect( new MappingSources().addManagedClasses( types ), new PhysicalNamingStrategyStandardImpl(), check );
	}

	void inspect(MappingSources sources, PhysicalNamingStrategy physical, Consumer<Metadata> check) {
		try ( var registry = ServiceRegistryUtil.serviceRegistry() ) {
			var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry, sources, strategy(), physical );
			((MetadataImplementor) metadata).orderColumns( false );
			((MetadataImplementor) metadata).validate();
			check.accept( metadata );
		}
	}

	static org.hibernate.mapping.PersistentClass entity(Metadata metadata, Class<?> type) {
		return metadata.getEntityBinding( type.getName() );
	}

	static org.hibernate.mapping.Collection collection(Metadata metadata, Class<?> type, String path) {
		return metadata.getCollectionBinding( type.getName() + "." + path );
	}

	static void columns(Value value, String... names) {
		assertThat( value.getColumns() ).extracting( org.hibernate.mapping.Column::getName )
				.containsExactlyInAnyOrder( names );
	}

	/// [Table#name()] uses the entity name; [Column#name()] uses the persistent
	/// field name, including inherited id/version fields. Explicit overrides win.
	@Test
	void entityNamesAndInheritedFields() {
		inspect( metadata -> {
			assertThat( entity( metadata, NamingDefaultEntity.class ).getTable().getName() ).isEqualTo( "NamingDefaultEntity" );
			var alias = entity( metadata, Aliased.class );
			assertThat( alias.getTable().getName() ).isEqualTo( "PublicAlias" );
			columns( alias.getIdentifier(), "record_id" );
			columns( alias.getVersion().getValue(), "revision" );
			columns( alias.getProperty( "description" ).getValue(), "description" );
			columns( alias.getProperty( "inherited" ).getValue(), "inherited_override" );
			assertThat( entity( metadata, ExplicitTable.class ).getTable().getName() ).isEqualTo( "chosen_table" );
		}, NamingDefaultEntity.class, Aliased.class, ExplicitTable.class );
	}

	/// [Column#name()] follows the property name under PROPERTY access,
	/// independently of the backing field's spelling.
	@Test
	void propertyAccessUsesPropertyNames() {
		inspect( metadata -> {
			var model = entity( metadata, PropertyEntity.class );
			columns( model.getIdentifier(), "key" );
			columns( model.getProperty( "label" ).getValue(), "label" );
		}, PropertyEntity.class );
	}

	/// [JoinColumn#name()] combines the relationship attribute and referenced PK
	/// column, for both one-to-one and many-to-one; an explicit join name wins.
	@Test
	void toOneUsesRenamedTargetPrimaryKey() {
		inspect( metadata -> {
			var model = entity( metadata, ToOneOwner.class );
			columns( model.getProperty( "address" ).getValue(), "address_target_pk" );
			columns( model.getProperty( "detail" ).getValue(), "detail_target_pk" );
			columns( model.getProperty( "explicit" ).getValue(), "selected_fk" );
		}, ToOneOwner.class, Target.class );
	}

	/// JPA 4.0-M4 section 2.12.1: the inverse one-to-one reuses the owning
	/// association, without generating another FK column.
	@Test
	void bidirectionalOneToOneHasOneOwningColumn() {
		inspect( metadata -> {
			columns( entity( metadata, OneOwner.class ).getProperty( "detail" ).getValue(), "detail_detail_pk" );
			assertThat( entity( metadata, OneDetail.class ).getTable().getColumns() )
					.extracting( org.hibernate.mapping.Column::getName ).containsExactly( "detail_pk" );
		}, OneOwner.class, OneDetail.class );
	}

	/// JPA 4.0-M4 section 2.12.2: mappedBy identifies the existing owner FK;
	/// neither registration order nor the inverse collection name creates a new one.
	@Test
	void inverseOneToManyReusesOwnerForeignKey() {
		for ( boolean reverse : new boolean[] {false, true} ) {
			inspect( metadata -> {
				var children = collection( metadata, Parent.class, "children" );
				assertThat( children.isInverse() ).isTrue();
				columns( children.getKey(), "parent_parent_pk" );
				columns( entity( metadata, Child.class ).getProperty( "parent" ).getValue(), "parent_parent_pk" );
				assertThat( children.getCollectionTable() ).isSameAs( entity( metadata, Child.class ).getTable() );
			}, reverse ? Child.class : Parent.class, reverse ? Parent.class : Child.class );
		}
	}

	/// JPA 4.0-M4 section 2.12.5: a unidirectional one-to-many defaults to a
	/// join table; [JoinColumn] instead selects a FK in the target table.
	@Test
	void unidirectionalOneToManyStorageAndNames() {
		inspect( metadata -> {
			var joined = collection( metadata, UniOwner.class, "joined" );
			assertThat( joined.getCollectionTable().getName() ).isEqualTo( "uni_owner_targets" );
			columns( joined.getKey(), "UniOwner_owner_pk" );
			columns( joined.getElement(), "joined_target_pk" );
			var direct = collection( metadata, UniOwner.class, "direct" );
			assertThat( direct.getCollectionTable() ).isSameAs( entity( metadata, Target.class ).getTable() );
			columns( direct.getKey(), "direct_owner_pk" );
		}, UniOwner.class, Target.class );
	}

	/// [JoinTable#name()] composes primary table names, while relationship FK
	/// defaults use attribute/entity names, not an overridden primary table name.
	@Test
	void bidirectionalManyToManyUsesBothRelationshipNames() {
		for ( boolean reverse : new boolean[] {false, true} ) {
			inspect( metadata -> {
				var owning = collection( metadata, Author.class, "books" );
				var inverse = collection( metadata, Book.class, "writers" );
				assertThat( owning.getCollectionTable().getName() ).isEqualTo( "author_table_book_table" );
				assertThat( inverse.getCollectionTable() ).isSameAs( owning.getCollectionTable() );
				columns( owning.getKey(), "writers_author_pk" );
				columns( owning.getElement(), "books_book_pk" );
				columns( inverse.getKey(), "books_book_pk" );
				columns( inverse.getElement(), "writers_author_pk" );
			}, reverse ? Book.class : Author.class, reverse ? Author.class : Book.class );
		}
	}

	/// JPA relationship defaults also apply to self-references; distinct owner
	/// and relationship prefixes distinguish the two FK columns.
	@Test
	void selfReferencingJoinTable() {
		inspect( metadata -> {
			var links = collection( metadata, Node.class, "peers" );
			assertThat( links.getCollectionTable().getName() ).isEqualTo( "node_table_node_table" );
			columns( links.getKey(), "Node_node_pk" );
			columns( links.getElement(), "peers_node_pk" );
		}, Node.class );
	}

	/// [CollectionTable#name()], [MapKeyColumn#name()], [MapKeyJoinColumn#name()],
	/// and [OrderColumn#name()] each have distinct defaults. Borrowed map keys
	/// reuse the value's property column and must not add a KEY column.
	@Test
	void collectionAndMapDefaults() {
		inspect( metadata -> {
			var tags = collection( metadata, CollectionOwner.class, "tags" );
			assertThat( tags.getCollectionTable().getName() ).isEqualTo( "CollectionAlias_tags" );
			columns( tags.getKey(), "CollectionAlias_owner_pk" );
			columns( tags.getElement(), "tags" );
			columns( ((org.hibernate.mapping.List) tags).getIndex(), "tags_ORDER" );
			var labels = (org.hibernate.mapping.Map) collection( metadata, CollectionOwner.class, "labels" );
			columns( labels.getIndex(), "labels_KEY" );
			columns( labels.getElement(), "labels" );
			var entities = (org.hibernate.mapping.Map) collection( metadata, CollectionOwner.class, "byTarget" );
			columns( entities.getIndex(), "byTarget_KEY" );
			var explicit = (org.hibernate.mapping.Map) collection( metadata, CollectionOwner.class, "explicitKeys" );
			columns( explicit.getIndex(), "chosen_key" );
			var borrowed = (org.hibernate.mapping.Map) collection( metadata, CollectionOwner.class, "borrowed" );
			columns( borrowed.getIndex(), "code" );
			assertThat( borrowed.getCollectionTable().getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.doesNotContain( "borrowed_KEY" );
		}, CollectionOwner.class, Target.class );
	}

	/// [PrimaryKeyJoinColumn#name()] preserves the primary table's PK name in
	/// both joined inheritance and secondary tables.
	@Test
	void joinedAndSecondaryTablePrimaryKeys() {
		inspect( metadata -> {
			var sub = (org.hibernate.mapping.JoinedSubclass) entity( metadata, JoinedChild.class );
			columns( sub.getKey(), "base_pk" );
			var secondary = entity( metadata, Secondary.class ).getJoins().get( 0 );
			assertThat( secondary.getTable().getName() ).isEqualTo( "secondary_details" );
			columns( secondary.getKey(), "secondary_pk" );
			columns( entity( metadata, Secondary.class ).getProperty( "text" ).getValue(), "text" );
		}, JoinedBase.class, JoinedChild.class, Secondary.class );
	}

	/// [DiscriminatorColumn#name()] defaults to DTYPE both when absent and
	/// when an annotation supplies only length; an explicit name takes precedence.
	@Test
	void singleTableDiscriminatorDefaults() {
		inspect( metadata -> {
			columns( entity( metadata, SingleBase.class ).getDiscriminator(), "DTYPE" );
			assertThat( entity( metadata, SingleChild.class ).getTable() ).isSameAs( entity( metadata, SingleBase.class ).getTable() );
			columns( entity( metadata, DeclaredBase.class ).getDiscriminator(), "DTYPE" );
			assertThat( entity( metadata, DeclaredBase.class ).getDiscriminator().getColumns().get( 0 ).getLength() ).isEqualTo( 64 );
			columns( entity( metadata, NamedBase.class ).getDiscriminator(), "kind" );
		}, SingleBase.class, SingleChild.class, DeclaredBase.class, DeclaredChild.class, NamedBase.class, NamedChild.class );
	}

	/// [InheritanceType#TABLE_PER_CLASS] retains inherited column names in each
	/// concrete table, whose default name comes from its own entity name.
	@Test
	void tablePerClassRetainsInheritedNames() {
		inspect( metadata -> {
			var child = entity( metadata, Concrete.class );
			assertThat( child.getTable().getName() ).isEqualTo( "Concrete" );
			assertThat( child.getTable().getColumns() ).extracting( org.hibernate.mapping.Column::getName )
					.contains( "root_pk", "inheritedText", "detail" );
		}, AbstractEntity.class, Concrete.class );
	}

	/// [MapsId] shares the explicitly named relationship column with the id;
	/// composite joins explicitly name both columns as required for portability.
	@Test
	void derivedIdentityReusesDeclaredJoinColumns() {
		inspect( metadata -> {
			var simple = entity( metadata, Derived.class );
			columns( simple.getIdentifier(), "parent_fk" );
			columns( simple.getProperty( "parent" ).getValue(), "parent_fk" );
			assertThat( simple.getTable().getColumns() ).hasSize( 1 );
			var composite = entity( metadata, CompositeDerived.class );
			columns( composite.getIdentifier(), "fk_region", "fk_number" );
			columns( composite.getProperty( "parent" ).getValue(), "fk_region", "fk_number" );
			assertThat( composite.getTable().getColumns() ).hasSize( 2 );
		}, Target.class, Derived.class, CompositeParent.class, CompositeDerived.class );
	}

	/// [IdClass] uses the entity attributes' column mappings, and does not
	/// introduce an identifier-mapper prefix into explicitly named columns.
	@Test
	void idClassKeepsDeclaredColumns() {
		inspect( metadata -> columns( entity( metadata, IdClassEntity.class ).getIdentifier(), "region", "number" ), IdClassEntity.class );
	}

	/// JPA 4.0-M4 section 2.15: explicit double-quoted identifiers retain case
	/// and quoting, with schema and catalog kept separate from the table identifier.
	@Test
	void quotedIdentifiersAndNamespaces() {
		inspect( metadata -> {
			var model = entity( metadata, Quoted.class );
			assertThat( model.getTable().getName() ).isEqualTo( "MixedTable" );
			assertThat( model.getTable().isQuoted() ).isTrue();
			assertThat( model.getTable().getSchema() ).isEqualTo( "MixedSchema" );
			assertThat( model.getTable().getCatalog() ).isEqualTo( "MixedCatalog" );
			var column = model.getProperty( "text" ).getColumns().get( 0 );
			assertThat( column.getName() ).isEqualTo( "MixedColumn" );
			assertThat( column.isQuoted() ).isTrue();
		}, Quoted.class );
	}

	/// Explicit map key/value [AttributeOverride] paths apply independently to
	/// embeddable keys and values, without merging their identically named members.
	@Test
	void embeddableMapOverrides() {
		inspect( metadata -> {
			var map = (org.hibernate.mapping.Map) collection( metadata, EmbeddedMapOwner.class, "entries" );
			columns( map.getIndex(), "key_code" );
			columns( map.getElement(), "value_code" );
		}, EmbeddedMapOwner.class );
	}

	/// JPA section 2.12 mapping defaults also apply to XML. Metadata-complete
	/// XML and annotations independently produce these literal expected names.
	@Test
	void xmlAndAnnotationDefaultsAgree() {
		for ( boolean xml : new boolean[] {false, true} ) {
			var sources = xml ? new MappingSources().addMappingResource(
					"org/hibernate/orm/test/namingstrategy/supported-naming.orm.xml" )
					: new MappingSources().addManagedClasses( XmlOwner.class, XmlTarget.class );
			inspect( sources, new PhysicalNamingStrategyStandardImpl(), metadata -> {
				var ownerType = xml ? XmlOnlyOwner.class : XmlOwner.class;
				var owner = entity( metadata, ownerType );
				assertThat( owner.getTable().getName() ).isEqualTo( "XmlOwner" );
				columns( owner.getIdentifier(), "id" );
				columns( owner.getProperty( "text" ).getValue(), "text" );
				columns( owner.getProperty( "target" ).getValue(), "target_target_pk" );
				columns( owner.getRecursiveProperty( "value.code" ).getValue(), "overridden_code" );
				var tags = collection( metadata, ownerType, "tags" );
				assertThat( tags.getCollectionTable().getName() ).isEqualTo( "XmlOwner_tags" );
				columns( tags.getKey(), "XmlOwner_id" );
				columns( tags.getElement(), "tags" );
			} );
		}
	}

	/// XML persistence-unit defaults supply namespaces and delimited identifier
	/// treatment (JPA section 2.15), including for implicitly named objects.
	@Test
	void xmlDelimitedIdentifiersAndDefaultNamespaces() {
		var sources = new MappingSources().addManagedClasses( AbstractEntity.class, Concrete.class, Secondary.class )
				.addMappingResource( "org/hibernate/orm/test/namingstrategy/supported-naming.orm.xml" )
				.addMappingResource( "org/hibernate/orm/test/namingstrategy/supported-naming-defaults.orm.xml" );
		inspect( sources, new PhysicalNamingStrategyStandardImpl(), metadata -> {
			var table = entity( metadata, XmlOnlyOwner.class ).getTable();
			assertThat( table.getName() ).isEqualTo( "XmlOwner" );
			assertThat( table.isQuoted() ).isTrue();
			assertThat( table.getColumns() ).allSatisfy( column -> assertThat( column.isQuoted() ).isTrue() );
			var tables = java.util.List.of( table,
					entity( metadata, Concrete.class ).getTable(),
					entity( metadata, Secondary.class ).getJoins().get( 0 ).getTable(),
					collection( metadata, XmlOnlyOwner.class, "tags" ).getCollectionTable() );
			org.assertj.core.api.SoftAssertions.assertSoftly( softly -> {
				for ( var namedTable : tables ) {
					softly.assertThat( namedTable.getSchema() ).as( "%s schema", namedTable.getName() ).isEqualTo( "NamingSchema" );
					softly.assertThat( namedTable.isSchemaQuoted() ).isTrue();
					softly.assertThat( namedTable.getCatalog() ).as( "%s catalog", namedTable.getName() ).isEqualTo( "NamingCatalog" );
					softly.assertThat( namedTable.isCatalogQuoted() ).isTrue();
				}
			} );
		} );
	}

	/// Generated schema must support persistence and navigation through a shared
	/// inverse FK and a MapsId column, not just expose plausible metadata names.
	@Test
	void sharedJoinAndDerivedIdentityWorkAtRuntime() {
		try ( var registry = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( "jakarta.persistence.schema-generation.database.action", "create-drop" ).build() ) {
			var metadata = MetadataBuildingTestHelper.buildMetadataWithNaming( registry,
					new MappingSources().addManagedClasses( Parent.class, Child.class, Target.class, Derived.class ),
					strategy(), new PhysicalNamingStrategyStandardImpl() );
			try ( var factory = org.hibernate.boot.pipeline.internal.SessionFactoryPipeline.build(
					metadata, new org.hibernate.boot.internal.SessionFactoryOptionsCollector() ) ) {
				try ( var session = factory.openSession() ) {
					var tx = session.beginTransaction();
					var parent = new Parent(); parent.id = 1;
					var child = new Child(); child.id = 2; child.parent = parent;
					var target = new Target(); target.id = 3;
					var derived = new Derived(); derived.parent = target;
					session.persist( parent ); session.persist( child );
					session.persist( target ); session.persist( derived );
					tx.commit();
				}
				try ( var session = factory.openSession() ) {
					var tx = session.beginTransaction();
					assertThat( session.find( Parent.class, 1L ).children ).singleElement()
							.satisfies( child -> assertThat( child.id ).isEqualTo( 2 ) );
					assertThat( session.getIdentifier( session.find( Derived.class, 3L ).parent ) ).isEqualTo( 3L );
					tx.commit();
				}
			}
		}
	}

	@MappedSuperclass
	static class Inherited { @Id @Column(name="record_id") long id; @Version int revision; String inherited; }
	@Entity(name="PublicAlias") @AttributeOverride(name="inherited", column=@Column(name="inherited_override"))
	static class Aliased extends Inherited { String description; }
	@Entity(name="IgnoredAlias") @Table(name="chosen_table")
	static class ExplicitTable { @Id long id; }
	@Entity(name="PropertyEntity") @Access(AccessType.PROPERTY)
	static class PropertyEntity {
		private long backingId;
		private String backingText;
		@Id public long getKey() { return backingId; }
		public void setKey(long value) { backingId=value; }
		public String getLabel() { return backingText; }
		public void setLabel(String value) { backingText=value; }
	}
	@Entity(name="TargetAlias") @Table(name="targets")
	static class Target { @Id @Column(name="target_pk") long id; String code; }
	@Entity(name="ToOneOwner")
	static class ToOneOwner {
		@Id long id;
		@ManyToOne Target address;
		@OneToOne Target detail;
		@ManyToOne @JoinColumn(name="selected_fk") Target explicit;
	}
	@Entity(name="OneOwner") static class OneOwner { @Id long id; @OneToOne OneDetail detail; }
	@Entity(name="OneDetail") static class OneDetail { @Id @Column(name="detail_pk") long id; @OneToOne(mappedBy="detail") OneOwner owner; }
	@Entity(name="Parent") static class Parent { @Id @Column(name="parent_pk") long id; @OneToMany(mappedBy="parent") Set<Child> children; }
	@Entity(name="Child") static class Child { @Id long id; @ManyToOne Parent parent; }
	@Entity(name="UniOwner") @Table(name="uni_owner")
	static class UniOwner { @Id @Column(name="owner_pk") long id; @OneToMany Set<Target> joined; @OneToMany @JoinColumn Set<Target> direct; }
	@Entity(name="Author") @Table(name="author_table")
	static class Author { @Id @Column(name="author_pk") long id; @ManyToMany Set<Book> books; }
	@Entity(name="Book") @Table(name="book_table")
	static class Book { @Id @Column(name="book_pk") long id; @ManyToMany(mappedBy="books") Set<Author> writers; }
	@Entity(name="Node") @Table(name="node_table")
	static class Node { @Id @Column(name="node_pk") long id; @ManyToMany Set<Node> peers; }
	@Entity(name="CollectionAlias") @Table(name="collection_owner")
	static class CollectionOwner {
		@Id @Column(name="owner_pk") long id;
		@ElementCollection @OrderColumn List<String> tags;
		@ElementCollection Map<String,String> labels;
		@ElementCollection Map<Target,String> byTarget;
		@ElementCollection @MapKeyColumn(name="chosen_key") Map<String,String> explicitKeys;
		@ManyToMany @MapKey(name="code") Map<String,Target> borrowed;
	}
	@Entity(name="JoinedBase") @Inheritance(strategy=InheritanceType.JOINED)
	static class JoinedBase { @Id @Column(name="base_pk") long id; }
	@Entity(name="JoinedChild") static class JoinedChild extends JoinedBase { String detail; }
	@Entity(name="Secondary") @SecondaryTable(name="secondary_details")
	static class Secondary { @Id @Column(name="secondary_pk") long id; @Column(table="secondary_details") String text; }
	@Entity(name="SingleBase") @Inheritance(strategy=InheritanceType.SINGLE_TABLE)
	static class SingleBase { @Id long id; }
	@Entity(name="SingleChild") static class SingleChild extends SingleBase { String detail; }
	@Entity(name="DeclaredBase") @Inheritance(strategy=InheritanceType.SINGLE_TABLE) @DiscriminatorColumn(length=64)
	static class DeclaredBase { @Id long id; }
	@Entity(name="DeclaredChild") static class DeclaredChild extends DeclaredBase { String detail; }
	@Entity(name="NamedBase") @Inheritance(strategy=InheritanceType.SINGLE_TABLE) @DiscriminatorColumn(name="kind")
	static class NamedBase { @Id long id; }
	@Entity(name="NamedChild") static class NamedChild extends NamedBase { String detail; }
	@Entity(name="AbstractEntity") @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
	abstract static class AbstractEntity { @Id @Column(name="root_pk") long id; String inheritedText; }
	@Entity(name="Concrete") static class Concrete extends AbstractEntity { String detail; }
	@Entity(name="Derived") static class Derived { @Id long id; @MapsId @ManyToOne @JoinColumn(name="parent_fk") Target parent; }
	@Embeddable static class CompositeKey implements Serializable { @Column(name="region") String region; @Column(name="number") long number; }
	@Entity(name="CompositeParent") static class CompositeParent { @EmbeddedId CompositeKey id; }
	@Entity(name="CompositeDerived") static class CompositeDerived {
		@EmbeddedId CompositeKey id;
		@MapsId @ManyToOne @JoinColumns({@JoinColumn(name="fk_region",referencedColumnName="region"), @JoinColumn(name="fk_number",referencedColumnName="number")}) CompositeParent parent;
	}
	static class IdClassKey implements Serializable { public String region; public long number; }
	@Entity(name="IdClassEntity") @IdClass(IdClassKey.class)
	static class IdClassEntity { @Id @Column(name="region") String region; @Id @Column(name="number") long number; }
	@Entity(name="Quoted") @Table(name="\"MixedTable\"",schema="\"MixedSchema\"",catalog="\"MixedCatalog\"")
	static class Quoted { @Id long id; @Column(name="\"MixedColumn\"") String text; }

	@Embeddable static class Location { String zip; @ManyToOne Target target; }
	@Embeddable static class Address { @Embedded Location location; @ElementCollection @OrderColumn List<String> tags; @ElementCollection Map<String,String> labels; @ElementCollection Map<Target,String> byTarget; @ManyToMany Set<Target> links; }
	@Entity(name="EmbeddedOwner") static class EmbeddedOwner { @Id long id; @Embedded Address home; }
	@Entity(name="OverriddenOwner") static class OverriddenOwner {
		@Id long id;
		@Embedded @AttributeOverride(name="location.zip",column=@Column(name="home_zip"))
		@AssociationOverride(name="location.target",joinColumns=@JoinColumn(name="home_target")) Address home;
	}
	@Embeddable static class Code implements Serializable { String code; }
	@Entity(name="EmbeddedMapOwner") static class EmbeddedMapOwner {
		@Id long id;
		@ElementCollection @AttributeOverrides({@AttributeOverride(name="key.code",column=@Column(name="key_code")), @AttributeOverride(name="value.code",column=@Column(name="value_code"))})
		Map<Code,Code> entries;
	}
	@Entity(name="XmlOwner") static class XmlOwner {
		@Id long id; String text;
		@ManyToOne XmlTarget target;
		@Embedded @AttributeOverride(name="code",column=@Column(name="overridden_code")) Code value;
		@ElementCollection Set<String> tags;
	}
	@Entity(name="XmlTarget") static class XmlTarget { @Id @Column(name="target_pk") long id; }
	@Embeddable static class SimpleLocation { String zip; }
	@Embeddable static class SimpleAddress { @Embedded SimpleLocation location; }
	@Entity(name="Repeated") static class Repeated { @Id long id; @Embedded SimpleAddress home; @Embedded SimpleAddress work; }
	@Entity(name="RepeatedOverrides") static class RepeatedOverrides {
		@Id long id;
		@Embedded @AttributeOverride(name="location.zip",column=@Column(name="home_zip")) SimpleAddress home;
		@Embedded @AttributeOverride(name="location.zip",column=@Column(name="work_zip")) SimpleAddress work;
	}
	@Embeddable static class ImplicitId implements Serializable { String region; long number; }
	@Entity(name="ImplicitIdOwner") static class ImplicitIdOwner { @EmbeddedId ImplicitId id; }

	// Deliberately unannotated: only the XML resource can supply their mappings.
	static class XmlOnlyOwner { long id; String text; XmlOnlyTarget target; XmlOnlyCode value; Set<String> tags; }
	static class XmlOnlyTarget { long id; }
	static class XmlOnlyCode { String code; }

	@Entity(name="ImplicitIdClassOwner") @IdClass(IdClassKey.class)
	static class ImplicitIdClassOwner { @Id String region; @Id long number; }
	@Entity(name="EmbeddedElements") static class EmbeddedElements { @Id long id; @ElementCollection Set<SimpleLocation> addresses; }

}

/// Top-level fixture makes the default unqualified entity name unambiguous.
///
/// @author Steve Ebersole
@Entity
class NamingDefaultEntity { @Id long id; }
