/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.collection.spi;

import org.hibernate.sqm.domain.PluralAttribute.ElementClassification;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public interface PluralAttributeElement<O extends Type, S extends org.hibernate.sqm.domain.Type> {
	ElementClassification getElementClassification();
	O getOrmType();
	S getSqmType();
}
