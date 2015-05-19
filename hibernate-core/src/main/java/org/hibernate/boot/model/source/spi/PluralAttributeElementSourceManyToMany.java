/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the source for the elements of persistent collections (plural
 * attributes) where the elements are many-to-many association
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementSourceManyToMany
		extends PluralAttributeElementSourceAssociation, RelationalValueSourceContainer,
				ForeignKeyContributingSource, Orderable {
	public String getReferencedEntityName();

	public String getReferencedEntityAttributeName();

	public boolean isIgnoreNotFound();

	public String getExplicitForeignKeyName();

	public boolean isUnique();

	public FilterSource[] getFilterSources();

	public String getWhere();

	public FetchCharacteristics getFetchCharacteristics();
}
