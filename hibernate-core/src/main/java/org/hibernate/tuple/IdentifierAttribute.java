/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
