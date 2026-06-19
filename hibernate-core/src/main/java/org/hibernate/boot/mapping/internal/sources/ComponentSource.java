/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.sources;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.TargetEmbeddable;
import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.categorize.StandardPersistentAttributeMemberResolver;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;
import org.hibernate.models.spi.TypeVariableScope;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.AssociationOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Embeddable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;

/// Source-model facts for an [org.hibernate.mapping.Component].
///
/// Components are path-sensitive: column overrides, association overrides, and converter
/// overrides are declared on the owning member but apply to nested members by path.  This
/// source object keeps the root member, component type, role, and path-aware override
/// collector together so [org.hibernate.boot.mapping.internal.binders.ComponentBinder] does
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
/// @author Steve Ebersole
/// @since 9.0
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

		/// Type-variable resolution scope for members of [#componentType()].
		///
		/// This is the scope in which generic member types should be interpreted for this
		/// particular component use.  It may differ from the raw component class when an
		/// embeddable or mapped-superclass member is inherited through a parameterized
		/// type, for example when `Embeddable<T>#value` is consumed as `String`.  Member
		/// discovery and nested component binding use this scope so association targets,
		/// implicit embedded members, and nested embeddable types are resolved from the
		/// applied component usage rather than from erased reflection types.
		TypeVariableScope typeVariableScope,

		/// Path-keyed mapping adjustments scoped to [#sourceMember()].
		///
		/// This is `null` for source shapes that do not currently expose path-based
		/// adjustment annotations through a member.
		PathAdjustmentCollector pathAdjustments,

		/// The default access type inherited from the owning managed type.
		AccessType defaultAccessType,

		/// Dot-separated attribute path prefix for this component source.
		String pathPrefix,

		/// Dot-separated owner-relative path prefix for implicit naming.
		String namingPathPrefix) {

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
				member.getType(),
				new PathAdjustmentCollector( member, bindingContext ),
				fallbackAccessType( member ),
				"",
				member.resolveAttributeName() + "."
		);
	}

	/// Creates a source for a normal embedded attribute, including owner-type
	/// path adjustments such as `@AttributeOverride(name = "home.city", ...)`.
	public static ComponentSource embeddedAttribute(
			MemberDetails member,
			ClassDetails ownerType,
			ClassDetails hierarchyRootType,
			AccessType defaultAccessType,
			BindingContext bindingContext) {
		return new ComponentSource(
				Kind.EMBEDDED_ATTRIBUTE,
				member,
				resolveEmbeddableType( member, ownerType, bindingContext ),
				member.resolveRelativeType( ownerType ),
				new PathAdjustmentCollector( member, ownerType, hierarchyRootType, bindingContext ),
				defaultAccessType,
				"",
				member.resolveAttributeName() + "."
		);
	}

	/// Creates a source for an embedded identifier component.
	///
	/// This currently only carries the component type.  If upstream stores source-model
	/// facts directly on mapping objects, identifier component sources should likely retain
	/// the identifier member or key-mapping metadata as well.
	public static ComponentSource embeddedIdentifier(ClassDetails componentType, AccessType defaultAccessType) {
		return new ComponentSource( Kind.EMBEDDED_IDENTIFIER, null, componentType, componentType, null,
				defaultAccessType, "", "" );
	}

	public static ComponentSource embeddedIdentifier(
			MemberDetails member,
			ClassDetails componentType,
			AccessType defaultAccessType,
			BindingContext bindingContext) {
		return embeddedIdentifier( member, componentType, member.getType(), defaultAccessType, bindingContext );
	}

	public static ComponentSource embeddedIdentifier(
			MemberDetails member,
			ClassDetails componentType,
			TypeVariableScope typeVariableScope,
			AccessType defaultAccessType,
			BindingContext bindingContext) {
		return new ComponentSource(
				Kind.EMBEDDED_IDENTIFIER,
				member,
				componentType,
				typeVariableScope,
				new PathAdjustmentCollector( member, bindingContext ),
				defaultAccessType,
				"",
				""
		);
	}

	/// Creates a source for an embeddable collection element.
	///
	/// The source member is the plural collection member, while the component type is the
	/// collection element type.
	public static ComponentSource collectionElement(
			MemberDetails member,
			AccessType defaultAccessType,
			BindingContext bindingContext) {
		return new ComponentSource(
				Kind.COLLECTION_ELEMENT,
				member,
				resolveEmbeddableType( member, bindingContext, true ),
				member.getElementType(),
				new PathAdjustmentCollector( member, bindingContext ),
				defaultAccessType,
				"",
				member.resolveAttributeName() + "."
		);
	}

	/// Creates a source for an embeddable map key.
	public static ComponentSource mapKey(
			MemberDetails member,
			ClassDetails componentType,
			AccessType defaultAccessType,
			BindingContext bindingContext) {
		return new ComponentSource(
				Kind.MAP_KEY,
				member,
				componentType,
				member.getMapKeyType(),
				new PathAdjustmentCollector( member, bindingContext ),
				defaultAccessType,
				"",
				member.resolveAttributeName() + ".key."
		);
	}

	public List<ComponentMember> members() {
		final Map<String, ComponentMember> members = new LinkedHashMap<>();
		collectMembers( componentType, members );
		if ( members.isEmpty() && kind == Kind.EMBEDDED_IDENTIFIER ) {
			collectPlainIdentifierClassMembers( componentType, members );
		}
		return new ArrayList<>( members.values() );
	}

	public List<ComponentMember> subclassMembers(BindingContext bindingContext) {
		final List<ComponentMember> members = new ArrayList<>();
		bindingContext.getCategorizedDomainModel().forEachEmbeddable( (name, embeddableType) -> {
			if ( isSubtypeOf( embeddableType, componentType ) ) {
				collectLocalMembers( embeddableType, embeddableType, members );
			}
		} );
		return members;
	}

	public ComponentSource nested(ComponentMember member, BindingContext bindingContext) {
		final ClassDetails nestedComponentType = resolveEmbeddableType( member, bindingContext );
		return new ComponentSource(
				kind,
				member.member(),
				nestedComponentType,
				member.member().resolveRelativeType( typeVariableScope ),
				pathAdjustments,
				defaultAccessType,
				member.path() + ".",
				member.fullPath() + "."
		);
	}

	public ComponentSource nested(
			MemberDetails member,
			TypeDetails memberType,
			String path,
			String fullPath,
			BindingContext bindingContext) {
		final ClassDetails nestedComponentType = resolveEmbeddableType( member, memberType, bindingContext );
		return new ComponentSource(
				kind,
				member,
				nestedComponentType,
				member.resolveRelativeType( typeVariableScope ),
				pathAdjustments,
				defaultAccessType,
				path + ".",
				fullPath + "."
		);
	}

	private static ClassDetails resolveEmbeddableType(ComponentMember member, BindingContext bindingContext) {
		return resolveEmbeddableType( member.member(), member.type(), bindingContext );
	}

	private static ClassDetails resolveEmbeddableType(
			MemberDetails member,
			TypeDetails memberType,
			BindingContext bindingContext) {
		final TargetEmbeddable targetEmbeddable = resolveTargetEmbeddable( member, false );
		if ( targetEmbeddable != null ) {
			return bindingContext.getClassDetailsRegistry()
					.resolveClassDetails( targetEmbeddable.value().getName() );
		}
		return memberType.determineRawClass();
	}

	public record ComponentMember(
			MemberDetails member,
			TypeDetails type,
			AttributePath relativePath,
			String path,
			AttributePath namingPath,
			String fullPath,
			ClassDetails declaringType,
			AccessType accessType) {
		public String attributeName() {
			return member.resolveAttributeName();
		}
	}

	private void collectMembers(ClassDetails componentClass, Map<String, ComponentMember> members) {
		final ClassDetails superClass = componentClass.getSuperClass();
		if ( isPersistentComponentSuperType( superClass ) ) {
			collectMembers( superClass, members );
		}

		final List<MemberDetails> localMembers = resolveLocalMembers( componentClass );
		for ( MemberDetails localMember : localMembers ) {
			addMember( localMember, typeVariableScope, componentClass, members );
		}
	}

	private void collectLocalMembers(
			ClassDetails componentClass,
			TypeVariableScope localTypeVariableScope,
			List<ComponentMember> members) {
		for ( MemberDetails localMember : resolveLocalMembers( componentClass ) ) {
			final String attributeName = localMember.resolveAttributeName();
			final String path = pathPrefix + attributeName;
			final String fullPath = namingPathPrefix + attributeName;
			members.add( new ComponentMember(
					localMember,
					localMember.resolveRelativeType( localTypeVariableScope ),
					AttributePath.parse( path ),
						path,
						AttributePath.parse( fullPath ),
						fullPath,
						componentClass,
						determineAccessType( componentClass )
				) );
		}
	}

	private List<MemberDetails> resolveLocalMembers(ClassDetails componentClass) {
		return StandardPersistentAttributeMemberResolver.INSTANCE.resolveAttributesMembers(
				componentClass,
				determineAccessType( componentClass ),
				(memberDetails) -> {
				},
				null
		);
	}

	private static boolean isSubtypeOf(ClassDetails subtype, ClassDetails supertype) {
		for ( ClassDetails candidate = subtype.getSuperClass(); candidate != null; candidate = candidate.getSuperClass() ) {
			if ( candidate.getName().equals( supertype.getName() ) ) {
				return true;
			}
		}
		return false;
	}

	private void addMember(
			MemberDetails localMember,
			TypeVariableScope localTypeVariableScope,
			ClassDetails declaringType,
			Map<String, ComponentMember> members) {
		final String attributeName = localMember.resolveAttributeName();
		final TypeDetails memberType = localMember.resolveRelativeType( localTypeVariableScope );
		final String path = pathPrefix + attributeName;
		final String fullPath = namingPathPrefix + attributeName;
		members.put( attributeName, new ComponentMember(
				localMember,
				memberType,
				AttributePath.parse( path ),
					path,
					AttributePath.parse( fullPath ),
					fullPath,
					declaringType,
					determineAccessType( declaringType )
			) );
	}

	private void collectPlainIdentifierClassMembers(ClassDetails componentClass, Map<String, ComponentMember> members) {
		for ( MemberDetails field : componentClass.getFields() ) {
			final String attributeName = field.resolveAttributeName();
			if ( attributeName == null || !field.isPersistable() || field.hasDirectAnnotationUsage(
					Transient.class ) ) {
				continue;
			}
			final TypeDetails fieldType = field.resolveRelativeType( typeVariableScope );
			final String path = pathPrefix + attributeName;
			final String fullPath = namingPathPrefix + attributeName;
			members.put( attributeName, new ComponentMember(
					field,
					fieldType,
					AttributePath.parse( path ),
						path,
						AttributePath.parse( fullPath ),
						fullPath,
						componentClass,
						determineAccessType( componentClass )
				) );
		}
	}

	private AccessType determineAccessType(ClassDetails componentClass) {
		final Access access = componentClass.getDirectAnnotationUsage( Access.class );
		return access == null ? defaultAccessType : access.value();
	}

	private static boolean isPersistentComponentSuperType(ClassDetails superClass) {
		return superClass != null
			&& superClass != ClassDetails.OBJECT_CLASS_DETAILS
			&& (superClass.hasDirectAnnotationUsage( MappedSuperclass.class )
				|| superClass.hasDirectAnnotationUsage( Embeddable.class ));
	}

	private static AccessType fallbackAccessType(MemberDetails member) {
		final Access access = member.getDeclaringType().getDirectAnnotationUsage( Access.class );
		return access == null ? AccessType.FIELD : access.value();
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

	private static ClassDetails resolveEmbeddableType(
			MemberDetails member,
			ClassDetails ownerType,
			BindingContext bindingContext) {
		final TargetEmbeddable targetEmbeddable = resolveTargetEmbeddable( member, false );
		if ( targetEmbeddable != null ) {
			return bindingContext.getClassDetailsRegistry()
					.resolveClassDetails( targetEmbeddable.value().getName() );
		}

		return member.resolveRelativeType( ownerType ).determineRawClass();
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
