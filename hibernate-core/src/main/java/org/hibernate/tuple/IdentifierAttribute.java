/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import org.hibernate.generator.Generator;
import org.hibernate.id.IdentifierGenerator;

/**
 * @deprecated No direct replacement; see {@link org.hibernate.metamodel.mapping.EntityIdentifierMapping}
 */
@Deprecated(forRemoval = true)
public interface IdentifierAttribute extends Attribute {
	boolean isVirtual();

	boolean isEmbedded();

	@Deprecated
	IdentifierGenerator getIdentifierGenerator();

	Generator getGenerator();

	boolean isIdentifierAssignedByInsert();

	boolean hasIdentifierMapper();
}
