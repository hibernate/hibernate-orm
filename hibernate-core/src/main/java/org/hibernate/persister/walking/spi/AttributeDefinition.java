/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.walking.spi;

import org.hibernate.metamodel.model.mapping.spi.ValueMapping;
import org.hibernate.type.Type;

/**
 * Descriptor for
 * @author Steve Ebersole
 */
public interface AttributeDefinition extends ValueMapping {
	AttributeSource getSource();
	String getName();
	Type getType();
	boolean isNullable();
}
