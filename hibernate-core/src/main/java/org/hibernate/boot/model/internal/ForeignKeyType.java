/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import org.hibernate.mapping.PersistentClass;

/**
 * Specifies the sort of foreign key reference given in a
 * {@link jakarta.persistence.JoinColumn} annotation.
 *
 * @see AnnotatedJoinColumns#getReferencedColumnsType(PersistentClass)
 *
 * @author Gavin King
 */
public enum ForeignKeyType {
	EXPLICIT_PRIMARY_KEY_REFERENCE,
	IMPLICIT_PRIMARY_KEY_REFERENCE,
	NON_PRIMARY_KEY_REFERENCE
}
