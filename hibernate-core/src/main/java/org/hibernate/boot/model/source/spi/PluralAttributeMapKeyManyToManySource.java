/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Additional source information for {@code <map-key-many-to-many/>} and
 * {@code <index-many-to-many/>}.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeMapKeyManyToManySource
		extends PluralAttributeMapKeySource, RelationalValueSourceContainer {
	String getReferencedEntityName();

	String getExplicitForeignKeyName();
}
