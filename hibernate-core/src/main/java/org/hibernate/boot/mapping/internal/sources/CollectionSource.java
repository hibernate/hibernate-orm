/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.sources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Bag;
import org.hibernate.annotations.CollectionId;
import org.hibernate.annotations.CollectionIdJavaClass;
import org.hibernate.annotations.CollectionIdJavaType;
import org.hibernate.annotations.CollectionIdJdbcType;
import org.hibernate.annotations.CollectionIdJdbcTypeCode;
import org.hibernate.annotations.CollectionIdMutability;
import org.hibernate.annotations.CollectionIdType;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.FetchProfileOverride;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterJoinTable;
import org.hibernate.annotations.ManyToAny;
import org.hibernate.annotations.SQLOrder;
import org.hibernate.annotations.SQLJoinTableRestriction;
import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.SortNatural;
import org.hibernate.boot.internal.Target;
import org.hibernate.boot.mapping.internal.binders.CascadeBinder;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.boot.models.internal.ModelsHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.models.internal.ClassTypeDetailsImpl;
import org.hibernate.models.internal.dynamic.DynamicClassDetails;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.ModelsContext;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapKey;
import jakarta.persistence.MapKeyClass;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyJoinColumn;
import jakarta.persistence.MapKeyJoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.OrderBy;
import org.jetbrains.annotations.NotNull;

/// Source-model facts for an [org.hibernate.mapping.Collection].
///
/// Like [BasicValueSource], this is a local prototype of information that may
/// eventually belong directly on collection mapping objects.  A plural member carries
/// several independent source concepts: the collection classification, the value type,
/// the optional map-key type, the collection-table declaration, and index/key annotations.
/// Keeping those facts together avoids re-deriving them from the Java collection class in
/// each binder step.
///
/// Historically, collection mapping had to support boot sources that did not have a
/// Hibernate Models member behind them.  In that world, the mapping object mostly held
/// the already-derived physical details: role name, collection table, key value, element
/// value, index value, and so on.  If upstream no longer needs to preserve legacy
/// `hbm.xml` as an equivalent source, collection mappings can retain their source model
/// identity and defer more interpretation until the mapping model has enough surrounding
/// context.
///
/// The key distinction is that the plural member is not the same thing as any one value
/// inside the collection.  Given `Map<K,V>`, the Java member contributes at least three
/// mapping concepts:
///
/// - the collection container, represented by [org.hibernate.mapping.Map]
/// - the collection element, represented by the mapping for `V`
/// - the collection index/key, represented by the mapping for `K`
///
/// Likewise, a `List<E>` contributes both the element value for `E` and a synthetic list
/// index value.  This record keeps those source-model facts together so downstream
/// binders do not need to recompute that `member.getElementType()` is the element source
/// while `member.getMapKeyType()` is the map-key source.
///
/// The [#classification()] is deliberately source-oriented.  It says what semantic
/// collection classification the source member requested: set, bag, list, map, and
/// eventually ordered/sorted/id-bag/array variants.  That is related to, but not identical
/// with, the concrete `org.hibernate.mapping.Collection` subclass.  For example, a Java
/// `List` is modeled as an indexed list, using explicit list-index metadata when
/// present and default `@OrderColumn` values otherwise.  Ordering annotations are
/// different: a list with `@OrderBy` or `@SQLOrder` and no list-index indicators is
/// order-driven rather than implicitly index-driven.  Keeping that decision on the
/// source object makes the classification explicit and testable.
///
/// In an upstream mapping-model version, a collection mapping might directly retain some
/// equivalent of:
///
/// - the plural source [MemberDetails]
/// - the effective element [TypeDetails]
/// - the effective map-key [TypeDetails], when applicable
/// - the source collection classification
/// - the collection table source annotation or implicit naming source
/// - the list-index or map-key column source annotation
///
/// That would make later naming, type resolution, and annotation interpretation depend
/// on source-model facts rather than repeated binder-local helper methods.
///
/// @since 9.0
/// @author Steve Ebersole
public record CollectionSource(
		/// The broad mapping nature represented by the plural member.
		Nature nature,

		/// The semantic collection classification requested by the source member.
		///
		/// This is the source-level decision that eventually drives which
		/// [org.hibernate.mapping.Collection] subclass is created.  Keeping it here makes
		/// rules such as "`List` means indexed list unless `@Bag` is present" explicit.
		CollectionClassification classification,

		/// The plural Hibernate Models member that contributed this collection mapping.
		///
		/// The member remains important even after the collection object exists because
		/// element annotations, collection table annotations, list-index annotations, and
		/// map-key annotations are all declared on this same Java member.
		MemberDetails member,

		/// The effective source type for the collection element.
		///
		/// For `Collection<E>`, `List<E>`, and `Set<E>`, this is `E`.  For `Map<K,V>`,
		/// this is `V`.  It is intentionally not the collection container type itself.
		TypeDetails elementType,

		/// The effective source type for the map key, or `null` for non-map collections.
		///
		/// This exists because map keys have their own basic-value binding concerns:
		/// `@MapKeyEnumerated`, `@MapKeyTemporal`, `@MapKeyColumn`, Hibernate
		/// `@MapKeyJavaType`, and similar annotations all describe this type, not the
		/// collection element type.
		TypeDetails mapKeyType,

		/// The `@CollectionTable` annotation declared on the plural member.
		///
		/// This is currently required to have an explicit name in the prototype binder.
		/// Longer term, the source should also be able to represent an implicit
		/// collection-table naming request with enough context for the naming strategy.
		CollectionTable collectionTable,

	/// The `@JoinTable` annotation declared on an association-valued plural member.
	JoinTable joinTable,

	/// The path-based association override currently active for this source, if any.
	AssociationOverride associationOverride,

	/// The path adjustment targeting this collection member directly.
	AttributeOverride attributeOverride,

		/// The models context used to resolve repeatable annotations.
		ModelsContext modelsContext) {

	/// Source-level plural mapping nature.
	public enum Nature {
		/// A value collection declared with `@ElementCollection`.
		ELEMENT_COLLECTION,

		/// An association collection declared with `@ManyToMany`.
		MANY_TO_MANY,

		/// An association collection declared with `@OneToMany`.
		ONE_TO_MANY,

		/// A heterogeneous association collection declared with Hibernate `@ManyToAny`.
		MANY_TO_ANY
	}

	/// Creates a collection source for an element collection member.
	///
	/// This method centralizes the collection-classification rules used by the binder.
	/// That is exactly the sort of logic that becomes easier to reason about if the
	/// upstream mapping model stores source facts directly instead of asking each binder
	/// to inspect Java collection classes and annotations independently.
	public static CollectionSource elementCollection(
			MemberDetails member,
			ModelsContext modelsContext) {
		return elementCollection( member, null, null, modelsContext );
	}

	/// Creates a collection source for an element collection member, including owner-level
	/// adjustments that target an inherited collection member directly.
	public static CollectionSource elementCollection(
			MemberDetails member,
			ClassDetails ownerType,
			ClassDetails hierarchyRootType,
			ModelsContext modelsContext) {
		final CollectionTable collectionTable = member.getDirectAnnotationUsage( CollectionTable.class );
		final CollectionClassification classification = determineClassification( member );
		final AssociationOverride associationOverride = locateAssociationOverride(
				member,
				ownerType,
				hierarchyRootType,
				modelsContext
		);

		final TypeDetails elementType = elementCollectionElementType( member, modelsContext );
		return new CollectionSource(
				elementType.determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Entity.class )
						? Nature.MANY_TO_MANY
						: Nature.ELEMENT_COLLECTION,
				classification,
				member,
					elementType,
					mapKeyType( member, classification, modelsContext ),
					collectionTable,
					effectiveJoinTable( member, associationOverride ),
					associationOverride,
					locateAttributeOverride( member, ownerType, hierarchyRootType, modelsContext ),
					modelsContext
			);
	}

	private static TypeDetails mapKeyType(
			MemberDetails member,
			CollectionClassification classification,
			ModelsContext modelsContext) {
		if ( classification.toJpaClassification() != jakarta.persistence.metamodel.PluralAttribute.CollectionType.MAP ) {
			return null;
		}
		final MapKeyClass mapKeyClass = member.getDirectAnnotationUsage( MapKeyClass.class );
		if ( mapKeyClass != null && mapKeyClass.value() != void.class && modelsContext != null ) {
			return new ClassTypeDetailsImpl(
					modelsContext.getClassDetailsRegistry()
							.resolveClassDetails( mapKeyClass.value().getName() ),
					TypeDetails.Kind.CLASS
			);
		}
		return member.getMapKeyType();
	}

	private static TypeDetails elementCollectionElementType(MemberDetails member, ModelsContext modelsContext) {
		final Target xmlTarget = member.getDirectAnnotationUsage( Target.class );
		if ( xmlTarget != null && modelsContext != null ) {
			return resolveXmlTargetType( xmlTarget, modelsContext );
		}
		final ElementCollection elementCollection = member.getDirectAnnotationUsage( ElementCollection.class );
		if ( elementCollection != null && elementCollection.targetClass() != void.class && modelsContext != null ) {
			return new ClassTypeDetailsImpl(
					modelsContext.getClassDetailsRegistry()
							.resolveClassDetails( elementCollection.targetClass().getName() ),
					TypeDetails.Kind.CLASS
			);
		}
		return member.getElementType();
	}

	/// Creates a collection source for an owning many-to-many association member.
	public static CollectionSource manyToMany(
			MemberDetails member,
			ModelsContext modelsContext) {
		return association( Nature.MANY_TO_MANY, member, modelsContext, null );
	}

	/// Creates a collection source for an owning many-to-many association member,
	/// applying an association override from an enclosing component path.
	public static CollectionSource manyToMany(
			MemberDetails member,
			AssociationOverride associationOverride,
			ModelsContext modelsContext) {
		return association( Nature.MANY_TO_MANY, member, modelsContext, associationOverride );
	}

	/// Creates a collection source for an owning one-to-many association member.
	public static CollectionSource oneToMany(
			MemberDetails member,
			ModelsContext modelsContext) {
		return association( Nature.ONE_TO_MANY, member, modelsContext, null );
	}

	/// Creates a collection source for an owning one-to-many association member,
	/// applying an association override from an enclosing component path.
	public static CollectionSource oneToMany(
			MemberDetails member,
			AssociationOverride associationOverride,
			ModelsContext modelsContext) {
		return association( Nature.ONE_TO_MANY, member, modelsContext, associationOverride );
	}

	/// Creates a collection source for a heterogeneous many-to-any association member.
	public static CollectionSource manyToAny(
			MemberDetails member,
			ModelsContext modelsContext) {
		return association( Nature.MANY_TO_ANY, member, modelsContext, null );
	}

	private static CollectionSource association(
			Nature nature,
			MemberDetails member,
			ModelsContext modelsContext,
			AssociationOverride associationOverride) {
		final CollectionSource source = elementCollection( member, modelsContext );
		final TypeDetails elementType = associationElementType( member, source.elementType, modelsContext );
		return new CollectionSource(
				nature,
				source.classification,
				source.member,
				elementType,
					source.mapKeyType,
					source.collectionTable,
					effectiveJoinTable( member, associationOverride ),
					associationOverride,
					source.attributeOverride,
					modelsContext
			);
	}

	private static JoinTable effectiveJoinTable(MemberDetails member, AssociationOverride associationOverride) {
		if ( associationOverride != null ) {
			return isSpecified( associationOverride.joinTable() ) ? associationOverride.joinTable() : null;
		}
		final JoinTable joinTable = member.getDirectAnnotationUsage( JoinTable.class );
		return isSpecified( joinTable ) ? joinTable : null;
	}

	private static boolean isSpecified(JoinTable joinTable) {
		return joinTable != null
				&& ( StringHelper.isNotEmpty( joinTable.name() )
						|| joinTable.joinColumns().length > 0
						|| joinTable.inverseJoinColumns().length > 0 );
	}

	private static TypeDetails associationElementType(
			MemberDetails member,
			TypeDetails fallback,
			ModelsContext modelsContext) {
		final Target xmlTarget = member.getDirectAnnotationUsage( Target.class );
		if ( xmlTarget != null && modelsContext != null ) {
			return resolveXmlTargetType( xmlTarget, modelsContext );
		}
		final ManyToMany manyToMany = member.getDirectAnnotationUsage( ManyToMany.class );
		if ( manyToMany != null && manyToMany.targetEntity() != void.class && modelsContext != null ) {
			return new ClassTypeDetailsImpl(
					modelsContext.getClassDetailsRegistry()
							.resolveClassDetails( manyToMany.targetEntity().getName() ),
					TypeDetails.Kind.CLASS
			);
		}
		final OneToMany oneToMany = member.getDirectAnnotationUsage( OneToMany.class );
		if ( oneToMany != null && oneToMany.targetEntity() != void.class && modelsContext != null ) {
			return new ClassTypeDetailsImpl(
					modelsContext.getClassDetailsRegistry()
							.resolveClassDetails( oneToMany.targetEntity().getName() ),
					TypeDetails.Kind.CLASS
			);
		}
		return fallback;
	}

	private static TypeDetails resolveXmlTargetType(Target xmlTarget, ModelsContext modelsContext) {
		final String targetName = xmlTarget.value();
		final ClassDetails classDetails = ModelsHelper.resolveClassDetails(
				targetName,
				modelsContext.getClassDetailsRegistry(),
				() -> new DynamicClassDetails( targetName, modelsContext )
		);
		return new ClassTypeDetailsImpl( classDetails, TypeDetails.Kind.CLASS );
	}

	private static CollectionClassification determineClassification(MemberDetails member) {
		final Class<?> collectionType = member.getType().determineRawClass().toJavaClass();
		if ( collectionType.isArray() ) {
			return CollectionClassification.ARRAY;
		}
		if ( isIdentifierBag( member ) ) {
			return CollectionClassification.ID_BAG;
		}
		if ( java.util.Set.class.isAssignableFrom( collectionType ) ) {
			if ( isSorted( member, collectionType ) ) {
				return CollectionClassification.SORTED_SET;
			}
			if ( isOrdered( member ) ) {
				return CollectionClassification.ORDERED_SET;
			}
			return CollectionClassification.SET;
		}
		if ( java.util.List.class.isAssignableFrom( collectionType )
				&& !member.hasDirectAnnotationUsage( Bag.class ) ) {
			if ( isOrdered( member ) && !hasListIndexIndicator( member ) ) {
				return CollectionClassification.BAG;
			}
			if ( isUnownedToMany( member ) && !hasListIndexIndicator( member ) ) {
				return CollectionClassification.BAG;
			}
			return CollectionClassification.LIST;
		}
		if ( java.util.Map.class.isAssignableFrom( collectionType ) ) {
			if ( isSorted( member, collectionType ) ) {
				return CollectionClassification.SORTED_MAP;
			}
			if ( isOrdered( member ) ) {
				return CollectionClassification.ORDERED_MAP;
			}
			return CollectionClassification.MAP;
		}
		return CollectionClassification.BAG;
	}

	private static AttributeOverride locateAttributeOverride(
			MemberDetails member,
			ClassDetails ownerType,
			ClassDetails hierarchyRootType,
			ModelsContext modelsContext) {
		AttributeOverride result = null;
		for ( ClassDetails type : ownerTypeChain( ownerType, hierarchyRootType ) ) {
			for ( AttributeOverride override : type.getRepeatedAnnotationUsages( AttributeOverride.class, modelsContext ) ) {
				if ( member.resolveAttributeName().equals( override.name() ) ) {
					result = override;
				}
			}
		}
		return result;
	}

	private static AssociationOverride locateAssociationOverride(
			MemberDetails member,
			ClassDetails ownerType,
			ClassDetails hierarchyRootType,
			ModelsContext modelsContext) {
		AssociationOverride result = null;
		for ( ClassDetails type : ownerTypeChain( ownerType, hierarchyRootType ) ) {
			for ( AssociationOverride override : type.getRepeatedAnnotationUsages( AssociationOverride.class, modelsContext ) ) {
				if ( member.resolveAttributeName().equals( override.name() ) ) {
					result = override;
				}
			}
		}
		return result;
	}

	private static List<ClassDetails> ownerTypeChain(ClassDetails ownerType, ClassDetails hierarchyRootType) {
		if ( ownerType == null ) {
			return List.of();
		}

		final ArrayList<ClassDetails> chain = new ArrayList<>();
		ClassDetails current = ownerType;
		while ( current != null && current != ClassDetails.OBJECT_CLASS_DETAILS ) {
			chain.add( 0, current );
			if ( sameClass( current, hierarchyRootType ) ) {
				break;
			}
			current = current.getSuperClass();
		}
		return chain;
	}

	private static boolean sameClass(ClassDetails one, ClassDetails another) {
		if ( one == null || another == null ) {
			return false;
		}

		final String oneClassName = one.getClassName();
		final String anotherClassName = another.getClassName();
		if ( oneClassName != null || anotherClassName != null ) {
			return Objects.equals( oneClassName, anotherClassName );
		}

		return Objects.equals( one.getName(), another.getName() );
	}

	public Column elementColumn() {
		return attributeOverride == null
				? member.getDirectAnnotationUsage( Column.class )
				: attributeOverride.column();
	}

	private static boolean isIdentifierBag(MemberDetails member) {
		return member.hasDirectAnnotationUsage( CollectionId.class )
				|| member.hasDirectAnnotationUsage( CollectionIdJavaClass.class )
				|| member.hasDirectAnnotationUsage( CollectionIdJavaType.class )
				|| member.hasDirectAnnotationUsage( CollectionIdJdbcType.class )
				|| member.hasDirectAnnotationUsage( CollectionIdJdbcTypeCode.class )
				|| member.hasDirectAnnotationUsage( CollectionIdMutability.class )
				|| member.hasDirectAnnotationUsage( CollectionIdType.class );
	}

	private static boolean isSorted(MemberDetails member, Class<?> collectionType) {
		return member.hasDirectAnnotationUsage( SortNatural.class )
				|| member.hasDirectAnnotationUsage( SortComparator.class )
				|| java.util.SortedSet.class.isAssignableFrom( collectionType )
				|| java.util.SortedMap.class.isAssignableFrom( collectionType );
	}

	private static boolean isOrdered(MemberDetails member) {
		return member.hasDirectAnnotationUsage( OrderBy.class )
				|| member.hasDirectAnnotationUsage( SQLOrder.class );
	}

	private static boolean hasListIndexIndicator(MemberDetails member) {
		return member.hasDirectAnnotationUsage( OrderColumn.class )
				|| member.hasDirectAnnotationUsage( org.hibernate.annotations.ListIndexBase.class )
				|| member.hasDirectAnnotationUsage( org.hibernate.annotations.ListIndexJavaType.class )
				|| member.hasDirectAnnotationUsage( org.hibernate.annotations.ListIndexJdbcType.class )
				|| member.hasDirectAnnotationUsage( org.hibernate.annotations.ListIndexJdbcTypeCode.class );
	}

	private static boolean isUnownedToMany(MemberDetails member) {
		final ManyToMany manyToMany = member.getDirectAnnotationUsage( ManyToMany.class );
		if ( manyToMany != null && StringHelper.isNotEmpty( manyToMany.mappedBy() ) ) {
			return true;
		}
		final OneToMany oneToMany = member.getDirectAnnotationUsage( OneToMany.class );
		return oneToMany != null && StringHelper.isNotEmpty( oneToMany.mappedBy() );
	}

	/// The direct `@ManyToMany` annotation.
	public ManyToMany manyToMany() {
		return member.getDirectAnnotationUsage( ManyToMany.class );
	}

	/// The direct `@OneToMany` annotation.
	public OneToMany oneToMany() {
		return member.getDirectAnnotationUsage( OneToMany.class );
	}

	/// The direct `@ManyToAny` annotation.
	public ManyToAny manyToAny() {
		return member.getDirectAnnotationUsage( ManyToAny.class );
	}

	private ElementCollection elementCollection() {
		return member.getDirectAnnotationUsage( ElementCollection.class );
	}

	/// Aggregates the JPA cascade and mapping defaults for association-valued plural mappings.
	public EnumSet<CascadeType> cascades(BindingState bindingState) {
		return switch ( nature ) {
			case MANY_TO_MANY -> manyToMany() == null
					? EnumSet.noneOf( CascadeType.class )
					: CascadeBinder.aggregateCascadeTypes( manyToMany().cascade(), false, bindingState );
			case ONE_TO_MANY -> CascadeBinder.aggregateCascadeTypes( oneToMany().cascade(), oneToMany().orphanRemoval(), bindingState );
			case MANY_TO_ANY -> CascadeBinder.aggregateCascadeTypes( manyToAny().cascade(), false, bindingState );
			case ELEMENT_COLLECTION -> EnumSet.noneOf( CascadeType.class );
		};
	}

	public boolean orphanRemoval() {
		return nature == Nature.ONE_TO_MANY && oneToMany().orphanRemoval();
	}

	public FetchType fetchType() {
		if ( hibernateFetchMode() == FetchMode.JOIN ) {
			return FetchType.EAGER;
		}
		final Fetch fetch = graphlessFetch();
		if ( fetch != null && fetch.type() != FetchType.DEFAULT ) {
			return fetch.type();
		}
		return switch ( nature ) {
			case MANY_TO_MANY -> manyToMany() == null
					? FetchType.LAZY
					: manyToMany().fetch();
			case ONE_TO_MANY -> oneToMany().fetch();
			case MANY_TO_ANY -> manyToAny().fetch();
			case ELEMENT_COLLECTION -> elementCollection() == null
					? FetchType.LAZY
					: elementCollection().fetch();
		};
	}

	public int batchSize() {
		final Fetch fetch = graphlessFetch();
		return fetch == null ? -1 : fetch.batchSize();
	}

	private Fetch graphlessFetch() {
		if ( modelsContext == null ) {
			final Fetch fetch = member.getDirectAnnotationUsage( Fetch.class );
			return fetch != null && fetch.graph().isEmpty() && fetch.subgraph().length == 0 ? fetch : null;
		}
		final Fetch[] fetches = member.getRepeatedAnnotationUsages( Fetch.class, modelsContext );
		for ( Fetch fetch : fetches ) {
			if ( fetch.graph().isEmpty() && fetch.subgraph().length == 0 ) {
				return fetch;
			}
		}
		return null;
	}

	public FetchMode hibernateFetchMode() {
		final org.hibernate.annotations.Fetch fetch = member.getDirectAnnotationUsage( org.hibernate.annotations.Fetch.class );
		return fetch == null ? null : fetch.value();
	}

	/// Whether the collection element value should be modeled as a component.
	///
	/// The source member can express embeddable-element intent in two ways: through the
	/// element type itself being annotated `@Embeddable`, or through `@Embedded` on the
	/// collection member.
	public boolean hasEmbeddableElement() {
		return !hasElementConversion()
				&& elementType.determineRawClass().hasDirectAnnotationUsage( Embeddable.class )
				|| member.hasDirectAnnotationUsage( Embedded.class );
	}

	private boolean hasElementConversion() {
		final Convert directConversion = member.getDirectAnnotationUsage( Convert.class );
		if ( isElementConversion( directConversion ) ) {
			return true;
		}
		for ( Convert conversion : member.getRepeatedAnnotationUsages( Convert.class, modelsContext ) ) {
			if ( isElementConversion( conversion ) ) {
				return true;
			}
		}
		return false;
	}

	private static boolean isElementConversion(Convert conversion) {
		if ( conversion == null
				|| conversion.disableConversion()
				|| conversion.converter() == AttributeConverter.class ) {
			return false;
		}
		final String attributeName = conversion.attributeName();
		return attributeName == null
				|| attributeName.isEmpty()
				|| "value".equals( attributeName );
	}

	/// The explicit list-index column source, if one was declared.
	///
	/// A missing annotation still represents a meaningful source request for lists:
	/// use the implicit/default index column.
	public OrderColumn orderColumn() {
		return member.getDirectAnnotationUsage( OrderColumn.class );
	}

	/// JPA order-by fragment declared for ordered sets/maps.
	public OrderBy orderBy() {
		return member.getDirectAnnotationUsage( OrderBy.class );
	}

	public SQLJoinTableRestriction sqlJoinTableRestriction() {
		return member.getDirectAnnotationUsage( SQLJoinTableRestriction.class );
	}

	public Filter[] filters() {
		return member.getRepeatedAnnotationUsages( Filter.class, modelsContext );
	}

	public FilterJoinTable[] filterJoinTables() {
		return member.getRepeatedAnnotationUsages( FilterJoinTable.class, modelsContext );
	}

	public FetchProfileOverride[] fetchProfileOverrides() {
		return member.getRepeatedAnnotationUsages( FetchProfileOverride.class, modelsContext );
	}

	/// Hibernate comparator declaration for sorted sets/maps.
	public SortComparator sortComparator() {
		return member.getDirectAnnotationUsage( SortComparator.class );
	}

	/// Hibernate base offset declaration for list/array index columns.
	public org.hibernate.annotations.ListIndexBase listIndexBase() {
		return member.getDirectAnnotationUsage( org.hibernate.annotations.ListIndexBase.class );
	}

	/// Hibernate custom collection type declaration.
	public org.hibernate.annotations.CollectionType collectionType() {
		return member.getDirectAnnotationUsage( org.hibernate.annotations.CollectionType.class );
	}

	/// The explicit map-key column source, if one was declared.
	///
	/// A missing annotation still represents a meaningful source request for maps:
	/// use the implicit/default map-key column.
	public MapKeyColumn mapKeyColumn() {
		return member.getDirectAnnotationUsage( MapKeyColumn.class );
	}

	/// The explicit property-based map key source, if declared.
	public MapKey mapKey() {
		return member.getDirectAnnotationUsage( MapKey.class );
	}

	/// The effective property name declared by `@MapKey`, honoring both the legacy
	/// `name` member and the shorthand `value` alias.
	public String mapKeyName() {
		final MapKey mapKey = mapKey();
		if ( mapKey == null ) {
			return null;
		}
		return !mapKey.value().isEmpty() ? mapKey.value() : mapKey.name();
	}

	/// The map-key join columns as a list, if an entity-valued map key was declared.
	public List<MapKeyJoinColumn> mapKeyJoinColumns() {
		final MapKeyJoinColumns plural = member.getDirectAnnotationUsage( MapKeyJoinColumns.class );
		if ( plural != null && plural.value().length > 0 ) {
			final ArrayList<MapKeyJoinColumn> result = new ArrayList<>( plural.value().length );
			result.addAll( Arrays.asList( plural.value() ) );
			return result;
		}

		final MapKeyJoinColumn singular = member.getDirectAnnotationUsage( MapKeyJoinColumn.class );
		return singular == null ? List.of() : List.of( singular );
	}

	public ForeignKeySource mapKeyForeignKeySource() {
		final MapKeyJoinColumns plural = member.getDirectAnnotationUsage( MapKeyJoinColumns.class );
		if ( plural != null ) {
			return ForeignKeySource.firstSpecified(
					ForeignKeySource.fromFirstSpecifiedMapKeyJoinColumn( mapKeyJoinColumns() ),
					ForeignKeySource.from( plural )
			);
		}
		final MapKeyJoinColumn singular = member.getDirectAnnotationUsage( MapKeyJoinColumn.class );
		return ForeignKeySource.from( singular );
	}

	/// The collection-table join columns as a list.
	///
	/// The conversion from annotation array to list is intentionally kept near the source
	/// object because these columns are source-level instructions for how the collection
	/// table joins back to its owner.
	public List<JoinColumn> joinColumns() {
		if ( joinTable != null ) {
			return extractJoinColumns( joinTable.joinColumns() );
		}
		if ( collectionTable == null ) {
			return List.of();
		}
		return extractJoinColumns( collectionTable.joinColumns() );
	}

	@NotNull
	private List<JoinColumn> extractJoinColumns(JoinColumn[] joinColumns) {
		if ( joinColumns.length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinColumns.length );
		result.addAll( Arrays.asList( joinColumns ) );
		return result;
	}

	/// The foreign-key columns declared directly on a unidirectional one-to-many association.
	public List<JoinColumn> oneToManyJoinColumns() {
		if ( associationOverride != null && associationOverride.joinColumns().length > 0 ) {
			return extractJoinColumns( associationOverride.joinColumns() );
		}
		final JoinColumns plural = member.getDirectAnnotationUsage( JoinColumns.class );
		if ( plural != null && plural.value().length > 0 ) {
			final ArrayList<JoinColumn> result = new ArrayList<>( plural.value().length );
			result.addAll( Arrays.asList( plural.value() ) );
			return result;
		}

		final JoinColumn singular = member.getDirectAnnotationUsage( JoinColumn.class );
		return singular == null ? List.of() : List.of( singular );
	}

	public ForeignKeySource oneToManyForeignKeySource() {
		if ( associationOverride != null ) {
			return ForeignKeySource.firstSpecified(
					ForeignKeySource.fromFirstSpecifiedJoinColumn( oneToManyJoinColumns() ),
					ForeignKeySource.from( associationOverride )
			);
		}
		final JoinColumns plural = member.getDirectAnnotationUsage( JoinColumns.class );
		if ( plural != null ) {
			return ForeignKeySource.firstSpecified(
					ForeignKeySource.fromFirstSpecifiedJoinColumn( oneToManyJoinColumns() ),
					ForeignKeySource.from( plural )
			);
		}
		return ForeignKeySource.fromFirstSpecifiedJoinColumn( oneToManyJoinColumns() );
	}

	/// The owning-side join columns for a join-table association.
	public List<JoinColumn> associationJoinColumns() {
		if ( joinTable != null ) {
			return extractJoinColumns( joinTable.joinColumns() );
		}
		if ( collectionTable == null || collectionTable.joinColumns().length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( collectionTable.joinColumns().length );
		result.addAll( Arrays.asList( collectionTable.joinColumns() ) );
		return result;
	}

	/// The target-side join columns for a join-table association.
	public List<JoinColumn> associationInverseJoinColumns() {
		if ( joinTable == null || joinTable.inverseJoinColumns().length == 0 ) {
			return List.of();
		}
		final ArrayList<JoinColumn> result = new ArrayList<>( joinTable.inverseJoinColumns().length );
		result.addAll( Arrays.asList( joinTable.inverseJoinColumns() ) );
		return result;
	}
}
