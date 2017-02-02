/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.id.IdentifierGenerator;

/**
 * @author Steve Ebersole
 */
public interface IdentifierAttribute extends Attribute, Property {
	boolean isVirtual();

	boolean isEmbedded();

	IdentifierValue getUnsavedValue();

	IdentifierGenerator getIdentifierGenerator();

	boolean isIdentifierAssignedByInsert();

	boolean hasIdentifierMapper();
}
