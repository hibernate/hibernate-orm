/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import org.hibernate.boot.model.source.spi.AttributePath;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.mapping.internal.sources.ComponentSource;
import org.hibernate.boot.mapping.internal.sources.CollectionSource;
import org.hibernate.boot.mapping.internal.sources.AnySource;
import org.hibernate.boot.mapping.internal.context.BindingContext;
import org.hibernate.boot.mapping.internal.context.BindingState;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeDetails;

import jakarta.persistence.AssociationOverride;

/// Binding-model usage node for one embeddable member at a specific embedded
/// site.
///
/// A component member binding is intentionally not just the declaration of a
/// member on an embeddable Java type.  It represents that member as applied at
/// one concrete embedded-site usage container, and is the embedded-site implementation of
/// [AttributeUsageBinding].  For example:
///
/// ```java
/// @Embeddable
/// class Address {
///     String city;
/// }
///
/// @Entity
/// class Customer {
///     @Embedded Address homeAddress;
///     @Embedded Address workAddress;
/// }
/// ```
///
/// There is one declaration of `Address#city`, but two component-member usage
/// bindings: `homeAddress.city` and `workAddress.city`.  Each can have different
/// path prefixes, override/conversion lookups, implicit-naming input,
/// association overrides, and type-variable scope.
///
/// The member binding records the source facts needed by component
/// materialization: source member, effective member type, path state,
/// component-member kind, overrides, conversions, and value intent.  The
/// effective member type is resolved for this embedded site, so it may differ
/// from the declaration's generic type variable when an embeddable or mapped
/// superclass is specialized through a generic subtype.
///
/// @since 9.0
/// @author Steve Ebersole
public class ComponentMemberBinding implements AttributeUsageBinding {
	private final MemberDetails member;
	private final TypeDetails type;
	private final String path;
	private final AttributePath namingPath;
	private final String fullPath;
	private final ClassDetails declaringType;
	private final AttributeDeclarationBinding declaration;
	private final AttributeUsageContainer usageContainer;
	private final AttributeNature nature;
	private final ValueIntent valueIntent;
	private final AssociationOverride associationOverride;
	private final String collation;

	private ComponentMemberBinding(
			MemberDetails member,
			TypeDetails type,
			String path,
			AttributePath namingPath,
			String fullPath,
			ClassDetails declaringType,
			AttributeDeclarationBinding declaration,
			AttributeUsageContainer usageContainer,
			AttributeNature nature,
			ValueIntent valueIntent,
			AssociationOverride associationOverride,
			String collation) {
		this.member = member;
		this.type = type;
		this.path = path;
		this.namingPath = namingPath;
		this.fullPath = fullPath;
		this.declaringType = declaringType;
		this.declaration = declaration;
		this.usageContainer = usageContainer;
		this.nature = nature;
		this.valueIntent = valueIntent;
		this.associationOverride = associationOverride;
		this.collation = collation;
	}

	public static ComponentMemberBinding from(
			ComponentSource source,
			ComponentSource.ComponentMember member,
			BindingState bindingState,
			BindingContext bindingContext) {
		final String path = member.path();
		final AttributeNature nature = determineNature( member.member(), member.type() );
		return new ComponentMemberBinding(
				member.member(),
				member.type(),
				path,
				member.namingPath(),
				member.fullPath(),
				member.declaringType(),
				resolveDeclaration( nature, member, bindingState ),
				new ComponentAttributeUsageContainer( source.componentType(), member.fullPath() ),
				nature,
				valueIntent( source, member, bindingState, bindingContext, nature ),
				source.associationOverride( path ),
				collation( member.member() )
		);
	}

	public MemberDetails member() {
		return member;
	}

	public TypeDetails type() {
		return type;
	}

	@Override
	public TypeDetails resolvedType() {
		return type;
	}

	/// Component-source-relative path used for override, conversion, and
	/// association-override lookup, for example `location.city`.
	///
	/// This path is string-based because annotation override names are
	/// string-based.
	public String path() {
		return path;
	}

	@Override
	public String attributePath() {
		return path;
	}

	/// Path passed to the configured implicit naming strategy.
	///
	/// Unlike [#path()], this is an [AttributePath] because naming strategies
	/// operate on Hibernate's path abstraction and may care about nested
	/// component structure.
	public AttributePath namingPath() {
		return namingPath;
	}

	/// Full source role for diagnostics and contribution ownership, including
	/// the owning attribute path outside this component when one exists.
	public String fullPath() {
		return fullPath;
	}

	@Override
	public String sourceRole() {
		return fullPath;
	}

	public ClassDetails declaringType() {
		return declaringType;
	}

	@Override
	public AttributeDeclarationBinding declaration() {
		return declaration;
	}

	@Override
	public AttributeUsageContainer usageContainer() {
		return usageContainer;
	}

	@Override
	public String attributeName() {
		return member.resolveAttributeName();
	}

	@Override
	public AttributeNature nature() {
		return nature;
	}

	@Override
	public ValueIntent valueIntent() {
		return valueIntent;
	}

	@Override
	public BasicValueIntent basicValueIntent() {
		return valueIntent instanceof BasicValueIntent basicValueIntent ? basicValueIntent : null;
	}

	@Override
	public EmbeddedValueIntent embeddedValueIntent() {
		return valueIntent instanceof EmbeddedValueIntent embeddedValueIntent ? embeddedValueIntent : null;
	}

	@Override
	public ToOneValueIntent toOneValueIntent() {
		return valueIntent instanceof ToOneValueIntent toOneValueIntent ? toOneValueIntent : null;
	}

	@Override
	public AnyValueIntent anyValueIntent() {
		return valueIntent instanceof AnyValueIntent anyValueIntent ? anyValueIntent : null;
	}

	public AssociationOverride associationOverride() {
		return associationOverride;
	}

	public String collation() {
		return collation;
	}

	private static AttributeNature determineNature(MemberDetails member, TypeDetails type) {
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.ManyToOne.class )
				|| member.hasDirectAnnotationUsage( jakarta.persistence.OneToOne.class ) ) {
			return AttributeNature.TO_ONE;
		}
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.ElementCollection.class ) ) {
			return AttributeNature.ELEMENT_COLLECTION;
		}
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.OneToMany.class ) ) {
			return AttributeNature.ONE_TO_MANY;
		}
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.ManyToMany.class ) ) {
			return AttributeNature.MANY_TO_MANY;
		}
		if ( member.hasDirectAnnotationUsage( org.hibernate.annotations.ManyToAny.class ) ) {
			return AttributeNature.MANY_TO_ANY;
		}
		if ( member.hasDirectAnnotationUsage( org.hibernate.annotations.Any.class ) ) {
			return AttributeNature.ANY;
		}
		if ( member.hasDirectAnnotationUsage( jakarta.persistence.Embedded.class )
				|| type.determineRawClass().hasDirectAnnotationUsage( jakarta.persistence.Embeddable.class ) ) {
			return AttributeNature.EMBEDDED;
		}
		return AttributeNature.BASIC;
	}

	private static ValueIntent valueIntent(
			ComponentSource source,
			ComponentSource.ComponentMember member,
			BindingState bindingState,
			BindingContext bindingContext,
			AttributeNature nature) {
		return switch ( nature ) {
			case BASIC -> BasicValueIntent.fromComponentMember( source, member, bindingState, bindingContext );
			case EMBEDDED -> EmbeddedValueIntent.fromComponentMember( member );
			case TO_ONE -> ToOneValueIntent.fromComponentMember( source, member );
			case ANY -> new AnyValueIntent( AnySource.create( member.member(), bindingContext, bindingState ) );
			case ELEMENT_COLLECTION, ONE_TO_MANY, MANY_TO_MANY, MANY_TO_ANY -> CollectionValueIntent.fromUsage(
					collectionSource( source, member, nature, bindingContext ),
					member.fullPath(),
					member.path(),
					bindingState,
					bindingContext
			);
			default -> null;
		};
	}

	private static CollectionSource collectionSource(
			ComponentSource source,
			ComponentSource.ComponentMember member,
			AttributeNature nature,
			BindingContext bindingContext) {
		final var modelsContext = bindingContext.getModelsContext();
		return switch ( nature ) {
			case ELEMENT_COLLECTION -> CollectionSource.elementCollection(
					member.member(),
					member.type(),
					source.componentType(),
					source.componentType(),
					modelsContext
			);
			case ONE_TO_MANY -> CollectionSource.oneToMany(
					member.member(),
					member.type(),
					source.componentType(),
					source.componentType(),
					source.associationOverride( member.path() ),
					modelsContext
			);
			case MANY_TO_MANY -> CollectionSource.manyToMany(
					member.member(),
					member.type(),
					source.componentType(),
					source.componentType(),
					source.associationOverride( member.path() ),
					modelsContext
			);
			case MANY_TO_ANY -> CollectionSource.manyToAny( member.member(), modelsContext );
			default -> throw new IllegalArgumentException( "Not a collection-valued component member - " + member.path() );
		};
	}

	private static String collation(MemberDetails member) {
		final org.hibernate.annotations.Collate collate =
				member.getDirectAnnotationUsage( org.hibernate.annotations.Collate.class );
		return collate == null ? null : collate.value();
	}

	private static AttributeDeclarationBinding resolveDeclaration(
			AttributeNature nature,
			ComponentSource.ComponentMember member,
			BindingState bindingState) {
		final AttributeDeclarationBinding declarationBinding = bindingState.getBootBindingModel()
				.findAttributeDeclaration( member.declaringType(), member.attributeName() );
		if ( declarationBinding != null ) {
			return declarationBinding;
		}
		return bindingState.getBootBindingModel()
				.findOrCreateAttributeDeclaration(
						member.declaringType(),
						member.member(),
						member.accessType(),
						nature
				);
	}
}
