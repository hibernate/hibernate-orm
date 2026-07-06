/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Objects;

import org.hibernate.boot.mapping.internal.model.BootBindingModel;
import org.hibernate.boot.mapping.internal.model.EntityIdentifierBinding;
import org.hibernate.boot.mapping.internal.model.IdentifierAttributeBinding;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;

/// Resolves applied identifier binding metadata from legacy identifier mapping
/// objects during runtime/JPA metamodel creation.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierHandoffResolver {
	private final BootBindingModel bootBindingModel;

	public IdentifierHandoffResolver(BootBindingModel bootBindingModel) {
		this.bootBindingModel = Objects.requireNonNull( bootBindingModel );
	}

	public @Nullable EntityIdentifierBinding findIdentifierBinding(PersistentClass persistentClass) {
		EntityIdentifierBinding binding = bootBindingModel.findEntityIdentifierBinding( persistentClass.getClassName() );
		if ( binding != null ) {
			return binding;
		}
		binding = bootBindingModel.findEntityIdentifierBinding( persistentClass.getEntityName() );
		if ( binding != null ) {
			return binding;
		}
		return bootBindingModel.findEntityIdentifierBinding( persistentClass.getJpaEntityName() );
	}

	/// Determines whether the identifier property should be registered as the
	/// concrete specialization of a generic mapped-superclass identifier.
	public boolean isConcreteGenericIdentifier(PersistentClass persistentClass, Property identifierProperty) {
		final EntityIdentifierBinding identifierBinding = findIdentifierBinding( persistentClass );
		if ( identifierBinding != null ) {
			final MemberDetails identifierMember = identifierBinding.identifierMember();
			if ( identifierMember != null
					&& identifierProperty.getName().equals( identifierMember.resolveAttributeName() ) ) {
				return isConcreteGenericUsage( identifierBinding, identifierMember );
			}

			final IdentifierAttributeBinding attributeBinding =
					identifierBinding.getAttribute( identifierProperty.getName() );
			if ( attributeBinding != null ) {
				return isConcreteGenericUsage( identifierBinding, attributeBinding.virtualMember() );
			}
		}
		return false;
	}

	private static boolean isConcreteGenericUsage(
			EntityIdentifierBinding identifierBinding,
			MemberDetails member) {
		final var declarationType = member.getType();
		final var usageType = member.resolveRelativeType( identifierBinding.owner().getClassDetails() );
		return AttributeTypeCorrespondence.isConcreteGenericUsage( declarationType, usageType );
	}
}
