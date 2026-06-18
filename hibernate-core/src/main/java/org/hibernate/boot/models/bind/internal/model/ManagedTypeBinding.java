/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.AccessType;

import org.hibernate.models.spi.ClassDetails;

/// Binding-model node for one managed type declaration.
///
/// A managed type binding records the boot-time interpretation of a Java type as
/// an entity, mapped superclass, or embeddable.  It owns the type's effective
/// access strategy and the persistent attributes declared or applied at this
/// point in the managed-type hierarchy.
///
/// @since 9.0
/// @author Steve Ebersole
public class ManagedTypeBinding {
	private final ClassDetails classDetails;
	private final Kind kind;
	private final AccessType accessType;
	private final List<AttributeBinding> declaredAttributes = new ArrayList<>();

	public ManagedTypeBinding(ClassDetails classDetails, Kind kind, AccessType accessType) {
		this.classDetails = classDetails;
		this.kind = kind;
		this.accessType = accessType;
	}

	public ClassDetails classDetails() {
		return classDetails;
	}

	public Kind kind() {
		return kind;
	}

	public AccessType accessType() {
		return accessType;
	}

	public void addDeclaredAttribute(AttributeBinding attributeBinding) {
		declaredAttributes.add( attributeBinding );
	}

	public List<AttributeBinding> declaredAttributes() {
		return declaredAttributes;
	}

	public enum Kind {
		ENTITY,
		MAPPED_SUPERCLASS,
		EMBEDDABLE
	}
}
