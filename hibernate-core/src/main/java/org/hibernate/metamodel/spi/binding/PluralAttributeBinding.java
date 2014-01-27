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
package org.hibernate.metamodel.spi.binding;

import java.util.Comparator;

import org.hibernate.metamodel.spi.domain.PluralAttribute;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * Describes the binding of a plural attribute.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeBinding extends AttributeBinding, Fetchable, Filterable {
	/**
	 * Retrieve the plural attribute being bound.
	 *
	 * @return The plural attribute descriptor
	 */
	@Override
	public PluralAttribute getAttribute();

	/**
	 * Retrieve the binding information pertaining to the collection (foreign) key.
	 *
	 * @return The key binding descriptor
	 */
	public PluralAttributeKeyBinding getPluralAttributeKeyBinding();

	/**
	 * Retrieve the binding information pertaining to the collection elements.
	 *
	 * @return The element binding descriptor
	 */
	public PluralAttributeElementBinding getPluralAttributeElementBinding();

	public boolean isMutable();

	public Caching getCaching();

	public Class<? extends CollectionPersister> getExplicitPersisterClass();

	public String getCustomLoaderName();

	public CustomSQL getCustomSqlInsert();

	public CustomSQL getCustomSqlUpdate();

	public CustomSQL getCustomSqlDelete();

	public CustomSQL getCustomSqlDeleteAll();

	String getWhere();

	boolean isSorted();

	boolean hasIndex();

	Comparator getComparator();

	int getBatchSize();

	String getOrderBy();

	String getReferencedPropertyName();
}
