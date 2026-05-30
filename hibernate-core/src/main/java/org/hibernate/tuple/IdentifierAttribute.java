/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import org.hibernate.generator.Generator;

/**
 * @deprecated No direct replacement; see {@link org.hibernate.metamodel.mapping.EntityIdentifierMapping}
 */
@Deprecated(forRemoval = true)
public interface IdentifierAttribute extends Attribute {
	boolean isVirtual();

	boolean isEmbedded();

	Generator getGenerator();

	boolean isIdentifierAssignedByInsert();

	boolean hasIdentifierMapper();
}
