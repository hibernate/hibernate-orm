/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
