/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the source mapping of plural-attribute (collection) foreign-key information.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeKeySource
		extends ForeignKeyContributingSource,
				RelationalValueSourceContainer {
	public String getReferencedPropertyName();
	public boolean isCascadeDeleteEnabled();
}
