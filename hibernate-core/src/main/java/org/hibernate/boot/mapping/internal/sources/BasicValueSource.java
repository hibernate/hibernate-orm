/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.sources;

import org.hibernate.annotations.CollectionIdJavaClass;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.Convert;
import jakarta.persistence.MapKeyClass;

/// Describes the source model object used to derive a [org.hibernate.mapping.BasicValue].
///
/// This is intentionally a small prototype of an idea that likely belongs closer to
/// `org.hibernate.mapping.BasicValue` upstream.  The historical mapping model was
/// shaped by the need to support multiple boot sources, especially legacy `hbm.xml`,
/// where there was no Hibernate Models [MemberDetails] or [TypeDetails] to
/// retain.  If upstream drops legacy `hbm.xml` support, the mapping model can stop
/// immediately erasing this source-model information and instead carry it as part of the
/// value itself.
///
/// The important distinction captured here is that a Java member can contribute several
/// different basic values, each with a different effective type and a different annotation
/// interpretation:
///
///   - a singular attribute maps the member type, [MemberDetails#getType()]
///   - an element collection maps the plural value type,
///     [MemberDetails#getElementType()]
///   - a map maps both an element value type and a map key type,
///     [MemberDetails#getMapKeyType()]
///   - a list index is a synthetic basic value derived from the plural attribute rather
///     than from a persistent Java member of its own
///
/// So the [#member()] answers "where did this basic value come from?", while
/// [#type()] answers "what source-model type is this specific value mapping?".
/// Those are the same for a normal basic attribute, but intentionally different for
/// collection elements and map keys.
///
/// The [#kind()] is not just metadata for dispatch.  It defines the annotation
/// vocabulary that is meaningful for the value.  For example, `@Enumerated` applies
/// to an attribute or collection element, while `@MapKeyEnumerated` applies to a map
/// key.  Likewise, Hibernate-specific annotations such as `@MapKeyJavaType` and
/// `@ListIndexJdbcTypeCode` are not naturally expressible if the binder only sees a
/// naked [org.hibernate.mapping.BasicValue].  Carrying the role/kind alongside the
/// source member lets one binding pipeline choose the correct annotation family.
///
/// In an upstream mapping-model version of this idea, `BasicValue` might directly
/// retain some equivalent of:
///
///   - the source [MemberDetails]
///   - the effective value [TypeDetails]
///   - the basic-value role/kind
///   - the converter source, when conversion was explicitly requested
///   - possibly the resolved explicit Java type override
///
/// That would let later type resolution work from source-model facts instead of captured
/// lambdas and ad hoc binder state.  This local record keeps the experiment scoped while
/// still making those upstream requirements visible.
///
/// @since 9.0
/// @author Steve Ebersole
public record BasicValueSource(
		/// The role this basic value plays relative to its source member.
		///
		/// A single [MemberDetails] may produce multiple [org.hibernate.mapping.BasicValue]
		/// instances with different roles.  The clearest example is `Map<K,V>`: the
		/// collection element value is one basic/component value, while the map key is an
		/// indexed basic value.  Both come from the same member, but their type and annotation
		/// rules are different.
		Kind kind,

		/// The Hibernate Models member that contributed this basic value.
		///
		/// This is deliberately retained even for synthetic values such as list indexes.  A
		/// list index does not have its own Java field, but its annotations
		/// (`@OrderColumn`, `@ListIndexJavaType`, `@ListIndexJdbcTypeCode`,
		/// etc.) live on the plural attribute member.  The same is true for map-key annotations.
		MemberDetails member,

		/// The effective source-model type of this specific basic value.
		///
		/// For [Kind#ATTRIBUTE], [Kind#EMBEDDABLE_MEMBER], and [Kind#IDENTIFIER]
		/// this is normally [MemberDetails#getType()].  For [Kind#COLLECTION_ELEMENT]
		/// it is [MemberDetails#getElementType()].  For [Kind#MAP_KEY] it is
		/// [MemberDetails#getMapKeyType()], unless an explicit Java class override is
		/// used.  [Kind#LIST_INDEX] currently has no [TypeDetails] because the
		/// index is synthetic and defaults to [Integer].
		TypeDetails type,

		/// Explicit Java type override for this value, if one is known before type resolution.
		///
		/// This currently models two slightly different ideas:
		///
		///   - synthetic values with a conventional Java type, such as a list index using
		///     [Integer]
		///   - annotation-driven overrides such as [MapKeyClass]
		///
		/// If this concept moves upstream, it may be better represented by a richer source
		/// descriptor rather than a raw [Class].  The prototype keeps it simple so we can
		/// see where the pressure appears.
		Class<?> explicitJavaType,

		/// Explicit converter annotation selected for this value, if one applies.
		///
		/// Conversion is source-role-sensitive in the same way enum, temporal, Java type,
		/// and JDBC type annotations are.  A direct `@Convert` on a singular basic member
		/// describes the attribute value; `@Convert(attributeName = "key")` describes a
		/// map key; `@Convert(attributeName = "value")` describes a collection element;
		/// and embeddable member conversion may be selected by a path override declared on
		/// the owning component member.  Keeping the selected conversion here lets
		/// [org.hibernate.boot.mapping.internal.binders.BasicValueBinder] apply
		/// conversion uniformly instead of having each higher-level binder special-case it.
		Convert conversion) {

	/// The basic-value role relative to the source member.
	///
	/// The names here intentionally describe source-model roles rather than physical mapping
	/// details.  For example, [#MAP_KEY] is currently stored as the
	/// [org.hibernate.mapping.IndexedCollection#getIndex()] value, but callers should
	/// not need to know that physical representation in order to decide whether
	/// `@MapKeyTemporal` applies.
	public enum Kind {
		/// A normal singular basic attribute.
		ATTRIBUTE,

		/// A basic member inside an embeddable/component.
		EMBEDDABLE_MEMBER,

		/// The value side of an element collection.
		///
		/// For a collection member such as `Set<String> names`, this maps
		/// `String`, not `Set`.  The source member remains the plural member
		/// because annotations such as `@Column`, `@Enumerated`, or
		/// `@Convert` are declared there for basic element collections.
		COLLECTION_ELEMENT,

		/// The synthetic index value of a list.
		///
		/// There is no persistent member corresponding to this value.  The source member is
		/// the list attribute, and the effective Java type is currently modeled as
		/// [Integer].  This role is where annotations such as Hibernate's
		/// `@ListIndexJavaType` and `@ListIndexJdbcTypeCode` are meaningful.
		LIST_INDEX,

		/// The key side of a map.
		///
		/// For `Map<K,V>`, this maps `K`.  The source member is still the plural
		/// map attribute because JPA and Hibernate map-key annotations live there.  This role
		/// is what lets the binder distinguish `@MapKeyEnumerated` from
		/// `@Enumerated`, `@MapKeyTemporal` from `@Temporal`, and so on.
		MAP_KEY,

		/// The synthetic identifier value of an id-bag.
		///
		/// The source member is the plural attribute, but this value is neither the
		/// collection element nor an index.  Its type and column details come from
		/// Hibernate's collection-id annotation family.
		COLLECTION_ID,

		/// A basic identifier value.
		///
		/// This currently behaves mostly like [#ATTRIBUTE], but it is kept distinct
		/// because identifier values have stricter rules for converters, mutability, nullability,
		/// and generated-value handling.  Keeping the role explicit avoids baking identifier
		/// exceptions into a generic attribute path.
		IDENTIFIER,

		/// The discriminator value of a Hibernate `@Any` association.
		///
		/// This is a basic value contributed by the association member, but its Java type
		/// comes from `@AnyDiscriminator` rather than the member's declared Java type.
		ANY_DISCRIMINATOR,

		/// The foreign-key value of a Hibernate `@Any` association.
		///
		/// This is the id value used to locate the selected target entity.  The Java type
		/// is source-level metadata supplied by `@AnyKeyJavaClass` or related Hibernate
		/// any-key annotations, not the declared Java type of the association member.
		ANY_KEY
	}

	/// Creates a source for a normal singular basic attribute.
	public static BasicValueSource attribute(MemberDetails member) {
		return new BasicValueSource( Kind.ATTRIBUTE, member, member.getType(), null, directConversion( member ) );
	}

	/// Creates a source for a normal singular basic attribute whose type has
	/// already been resolved for a concrete entity usage.
	public static BasicValueSource attribute(MemberDetails member, TypeDetails type) {
		return new BasicValueSource( Kind.ATTRIBUTE, member, type, null, directConversion( member ) );
	}

	/// Creates a source for a basic embeddable/component member.
	///
	/// This is distinct from [#attribute(MemberDetails)] so embeddable-member-specific
	/// override handling can be modeled explicitly later.  Today much of that is still
	/// handled by [ComponentBinder] before the basic value is created.
	public static BasicValueSource embeddableMember(MemberDetails member) {
		return embeddableMember( member, directConversion( member ) );
	}

	/// Creates a source for a basic embeddable/component member with the already-selected
	/// path-aware converter source.
	public static BasicValueSource embeddableMember(MemberDetails member, Convert conversion) {
		return new BasicValueSource( Kind.EMBEDDABLE_MEMBER, member, member.getType(), null, conversion );
	}

	public static BasicValueSource embeddableMember(MemberDetails member, TypeDetails type, Convert conversion) {
		return new BasicValueSource( Kind.EMBEDDABLE_MEMBER, member, type, null, conversion );
	}

	/// Creates a source for the basic value side of an element collection.
	public static BasicValueSource collectionElement(MemberDetails member) {
		return new BasicValueSource( Kind.COLLECTION_ELEMENT, member, member.getElementType(), null, directConversion( member ) );
	}

	/// Creates a source for the basic value side of an element collection whose effective
	/// element type has already been resolved from source-level metadata.
	public static BasicValueSource collectionElement(MemberDetails member, TypeDetails elementType) {
		return new BasicValueSource( Kind.COLLECTION_ELEMENT, member, elementType, null, directConversion( member ) );
	}

	/// Creates a source for the basic value side of an element collection with access to
	/// repeated `@Convert` declarations such as `@Convert(attributeName = "value", ...)`.
	public static BasicValueSource collectionElement(MemberDetails member, BindingContext bindingContext) {
		return collectionElement( member, member.getElementType(), bindingContext );
	}

	/// Creates a source for the basic value side of an element collection with an
	/// already-resolved effective element type and access to repeated `@Convert`
	/// declarations such as `@Convert(attributeName = "value", ...)`.
	public static BasicValueSource collectionElement(
			MemberDetails member,
			TypeDetails elementType,
			BindingContext bindingContext) {
		return new BasicValueSource(
				Kind.COLLECTION_ELEMENT,
				member,
				elementType,
				null,
				collectionRoleConversion( member, "value", bindingContext )
		);
	}

	/// Creates a source for a list index.
	///
	/// The index is synthetic.  We intentionally keep the source [MemberDetails] so
	/// index annotations on the list attribute can be consumed by the same basic-value binder.
	public static BasicValueSource listIndex(MemberDetails member) {
		return new BasicValueSource( Kind.LIST_INDEX, member, null, Integer.class, null );
	}

	/// Creates a source for a map key.
	///
	/// The effective type is normally [MemberDetails#getMapKeyType()].  JPA's
	/// [MapKeyClass] is represented as an explicit Java type override because it
	/// changes the key type used for the mapping independent of the declared generic.
	public static BasicValueSource mapKey(MemberDetails member) {
		return mapKey( member, null );
	}

	/// Creates a source for a map key with access to repeated `@Convert` declarations
	/// such as `@Convert(attributeName = "key", ...)`.
	public static BasicValueSource mapKey(MemberDetails member, BindingContext bindingContext) {
		final MapKeyClass mapKeyClass = member.getDirectAnnotationUsage( MapKeyClass.class );
		return new BasicValueSource(
				Kind.MAP_KEY,
				member,
				member.getMapKeyType(),
				mapKeyClass == null ? null : mapKeyClass.value(),
				collectionRoleConversion( member, "key", bindingContext )
		);
	}

	/// Creates a source for an id-bag collection identifier.
	public static BasicValueSource collectionId(MemberDetails member) {
		final CollectionIdJavaClass collectionIdJavaClass = member.getDirectAnnotationUsage( CollectionIdJavaClass.class );
		return new BasicValueSource(
				Kind.COLLECTION_ID,
				member,
				null,
				collectionIdJavaClass == null ? null : collectionIdJavaClass.idType(),
				null
		);
	}

	/// Creates a source for a basic identifier value.
	public static BasicValueSource identifier(MemberDetails member) {
		return new BasicValueSource( Kind.IDENTIFIER, member, member.getType(), null, directConversion( member ) );
	}

	/// Creates a source for a basic identifier value whose type has already been
	/// resolved for a concrete entity usage.
	public static BasicValueSource identifier(MemberDetails member, TypeDetails type) {
		return new BasicValueSource( Kind.IDENTIFIER, member, type, null, directConversion( member ) );
	}

	/// Creates a source for an `@Any` discriminator value.
	public static BasicValueSource anyDiscriminator(MemberDetails member, Class<?> discriminatorJavaType) {
		return new BasicValueSource( Kind.ANY_DISCRIMINATOR, member, null, discriminatorJavaType, null );
	}

	/// Creates a source for an `@Any` key value.
	public static BasicValueSource anyKey(MemberDetails member, Class<?> keyJavaType) {
		return new BasicValueSource( Kind.ANY_KEY, member, null, keyJavaType, null );
	}

	/// Resolves the source type to expose to [BasicValue].
	public BasicValue.SourceJavaType sourceJavaType() {
		return BasicValue.SourceJavaType.from( type, explicitJavaType );
	}

	public Class<?> rawJavaType() {
		return sourceJavaType().rawJavaClass();
	}

	private static Convert directConversion(MemberDetails member) {
		return member.getDirectAnnotationUsage( Convert.class );
	}

	private static Convert collectionRoleConversion(
			MemberDetails member,
			String roleName,
			BindingContext bindingContext) {
		if ( bindingContext != null ) {
			final var modelsContext = bindingContext.getBootstrapContext().getModelsContext();
			for ( Convert conversion : member.getRepeatedAnnotationUsages( Convert.class, modelsContext ) ) {
				if ( roleName.equals( conversion.attributeName() ) ) {
					return conversion;
				}
			}
		}

		final Convert directConversion = directConversion( member );
		if ( directConversion != null
				&& ( directConversion.attributeName() == null || directConversion.attributeName().isEmpty() ) ) {
			return directConversion;
		}
		return null;
	}
}
