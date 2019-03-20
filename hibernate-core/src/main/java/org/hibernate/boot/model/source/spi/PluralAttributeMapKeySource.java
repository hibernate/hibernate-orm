/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes source information about the key of a persistent map.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeMapKeySource extends PluralAttributeIndexSource {
	enum Nature {
		BASIC,
		EMBEDDED,
		MANY_TO_MANY,
		ANY
	}

	Nature getMapKeyNature();
}
