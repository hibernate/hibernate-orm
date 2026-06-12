/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.sources;

import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.boot.models.bind.spi.BindingContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;

import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;

/// Source-model facts for an [org.hibernate.mapping.Component].
///
/// Components are path-sensitive: column overrides, association overrides, and converter
/// overrides are declared on the owning member but apply to nested members by path.  This
/// source object keeps the root member, component type, role, and path-aware override
/// collector together so [org.hibernate.boot.models.bind.internal.binders.ComponentBinder] does
///  not need separate resolver lambdas for every kind of source.
///
/// This is another local prototype of information that may belong directly on
/// `org.hibernate.mapping.Component` upstream.  A component mapping is not just "a Java
/// class with properties".  It has a source role that changes how annotations are read
/// and how paths are interpreted.  An embedded attribute, an embedded identifier, a
/// nested component, and an embeddable collection element can all use the same Java
/// embeddable class while requiring different source context.
///
/// The most important source fact here is the path base.  Overrides such as
/// `@AttributeOverride(name = "location.city", ...)`,
/// `@AssociationOverride(name = "country", ...)`, and
/// `@Convert(attributeName = "location.city", ...)` are declared on the component's
/// owning member, not on the nested member where the mapping is ultimately created.
/// Without a component source, binders end up passing several parallel resolver lambdas
/// for column overrides, converter overrides, and association overrides.  Those lambdas
/// are really just different views of the same source object.
///
/// The [#sourceMember()] and [#componentType()] answer different questions:
///
/// - [#sourceMember()] is where path-based annotations and intent were declared
/// - [#componentType()] is the embeddable class whose members are being bound
///
/// They are usually related, but not interchangeable.  An embedded identifier source may
/// not have a source member in this prototype, while a collection element source uses the
/// plural collection member as its source even though the component type is the
/// collection element type.
///
/// In an upstream mapping-model version, a component mapping might directly retain:
///
/// - the source [MemberDetails], when one exists
/// - the component [ClassDetails]
/// - the component role/kind
/// - a path-aware override/conversion model
/// - enough path state to answer override lookups without binder-specific lambdas
///
/// That would make nested embeddables and collection-element embeddables much easier to
/// bind uniformly.
///
/// @since 9.0
/// @author Steve Ebersole
public record ComponentSource(
		/// The role this component plays relative to its source.
		Kind kind,

		/// The member that declared this component mapping, if there is one.
		///
		/// For embedded attributes and embeddable collection elements, this is the member on
		/// which path-based overrides are declared.  For an embedded identifier built from
		/// already categorized identifier metadata, this prototype may only have the
		/// component type.
		MemberDetails sourceMember,

		/// The embeddable/component class whose persistable members are being bound.
		ClassDetails componentType,

		/// Path-keyed mapping adjustments scoped to [#sourceMember()].
		///
		/// This is `null` for source shapes that do not currently expose path-based
		/// adjustment annotations through a member.
		PathAdjustmentCollector pathAdjustments) {
	/// Source-level component role.
	public enum Kind {
		/// A normal embedded/component-valued attribute.
		EMBEDDED_ATTRIBUTE,

		/// A component identifier value.
		EMBEDDED_IDENTIFIER,

		/// An embeddable value used as an element-collection element.
		COLLECTION_ELEMENT,

		/// An embeddable value used as a map key.
		MAP_KEY
	}

	/// Creates a source for a normal embedded attribute.
	///
	/// The owning member supplies both the component type and the path-based override
	/// annotations.
	public static ComponentSource embeddedAttribute(MemberDetails member, BindingContext bindingContext) {
		return new ComponentSource(
				Kind.EMBEDDED_ATTRIBUTE,
				member,
				resolveEmbeddableType( member, bindingContext, false ),
				new PathAdjustmentCollector( member, bindingContext )
		);
	}

	/// Creates a source for an embedded identifier component.
	///
	/// This currently only carries the component type.  If upstream stores source-model
	/// facts directly on mapping objects, identifier component sources should likely retain
	/// the identifier member or key-mapping metadata as well.
	public static ComponentSource embeddedIdentifier(ClassDetails componentType) {
		return new ComponentSource( Kind.EMBEDDED_IDENTIFIER, null, componentType, null );
	}

	/// Creates a source for an embeddable collection element.
	///
	/// The source member is the plural collection member, while the component type is the
	/// collection element type.
	public static ComponentSource collectionElement(MemberDetails member, BindingContext bindingContext) {
		return new ComponentSource(
				Kind.COLLECTION_ELEMENT,
				member,
				resolveEmbeddableType( member, bindingContext, true ),
				new PathAdjustmentCollector( member, bindingContext )
		);
	}

	/// Creates a source for an embeddable map key.
	public static ComponentSource mapKey(
			MemberDetails member,
			ClassDetails componentType,
			BindingContext bindingContext) {
		return new ComponentSource(
				Kind.MAP_KEY,
				member,
				componentType,
				new PathAdjustmentCollector( member, bindingContext )
		);
	}

	public static ClassDetails resolveEmbeddableType(
			MemberDetails member,
			BindingContext bindingContext,
			boolean collectionElement) {
		final TargetEmbeddable targetEmbeddable = resolveTargetEmbeddable( member, collectionElement );
		if ( targetEmbeddable != null ) {
			return bindingContext.getClassDetailsRegistry()
					.resolveClassDetails( targetEmbeddable.value().getName() );
		}

		return collectionElement
				? member.getElementType().determineRawClass()
				: member.getType().determineRawClass();
	}

	private static TargetEmbeddable resolveTargetEmbeddable(MemberDetails member, boolean collectionElement) {
		final TargetEmbeddable memberAnnotation = member.getDirectAnnotationUsage( TargetEmbeddable.class );
		if ( memberAnnotation != null ) {
			final boolean allowed = member.hasDirectAnnotationUsage( Embedded.class )
					|| member.hasDirectAnnotationUsage( ElementCollection.class );
			if ( !allowed ) {
				throw new MappingException( String.format(
						Locale.ROOT,
						"@TargetEmbeddable can only be specified on properties marked with @Embedded or @ElementCollection [%s#%s]",
						member.getDeclaringType().getName(),
						member.getName()
				) );
			}
			return memberAnnotation;
		}

		if ( collectionElement ) {
			return null;
		}

		final ClassDetails memberType = member.getType().determineRawClass();
		return memberType.isJdkClass() ? null : memberType.getDirectAnnotationUsage( TargetEmbeddable.class );
	}

	/// Resolves the column source for a component member path.
	///
	/// Path-based `@AttributeOverride` wins over the member's direct `@Column`.  Keeping
	/// this behavior on the source object makes it clear that override resolution is a
	/// source-model concern, not a physical-column concern.
	public ColumnSource columnSource(String path, MemberDetails member) {
		final var override = locateAttributeOverride( path );
		if ( override != null ) {
			return ColumnSource.from( override.column() );
		}

		final Column column = member.getDirectAnnotationUsage( Column.class );
		return ColumnSource.from( column );
	}

	/// Resolves the converter source for a component member path.
	///
	/// Path-based `@Convert(attributeName = ...)` wins over a direct converter declared on
	/// the nested member.  Direct converters with non-empty `attributeName` are ignored
	/// here because they are not direct conversions for the nested basic member.
	public Convert conversion(String path, MemberDetails member) {
		final Convert override = locateConversion( path );
		if ( override != null ) {
			return override;
		}

		final Convert directConversion = member.getDirectAnnotationUsage( Convert.class );
		return directConversion != null && StringHelper.isEmpty( directConversion.attributeName() )
				? directConversion
				: null;
	}

	/// Resolves an association override for a component member path.
	public AssociationOverride associationOverride(String path) {
		return locateAssociationOverride( path );
	}

	private jakarta.persistence.AttributeOverride locateAttributeOverride(String path) {
		if ( pathAdjustments == null ) {
			return null;
		}
		final var direct = pathAdjustments.locateAttributeOverride( path );
		return direct == null ? pathAdjustments.locateAttributeOverride( rolePath( path ) ) : direct;
	}

	private AssociationOverride locateAssociationOverride(String path) {
		if ( pathAdjustments == null ) {
			return null;
		}
		final var direct = pathAdjustments.locateAssociationOverride( path );
		return direct == null ? pathAdjustments.locateAssociationOverride( rolePath( path ) ) : direct;
	}

	private Convert locateConversion(String path) {
		if ( pathAdjustments == null ) {
			return null;
		}
		final var direct = pathAdjustments.locateConversion( path );
		return direct == null ? pathAdjustments.locateConversion( rolePath( path ) ) : direct;
	}

	private String rolePath(String path) {
		return switch ( kind ) {
			case MAP_KEY -> "key." + path;
			case COLLECTION_ELEMENT -> "value." + path;
			default -> path;
		};
	}
}
