/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.spi;

import org.hibernate.engine.FetchTiming;

/**
 * Describes the source for the elements of persistent collections (plural
 * attributes) where the elements are many-to-many association
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementSourceManyToMany
		extends PluralAttributeElementSource, AssociationSource, RelationalValueSourceContainer, ForeignKeyContributingSource, Orderable {
	public String getReferencedEntityName();

	public String getReferencedEntityAttributeName();

	public boolean isIgnoreNotFound();

	public String getExplicitForeignKeyName();

	public boolean isUnique();

	public FilterSource[] getFilterSources();

	public String getWhere();

	public FetchTiming getFetchTiming();
}
