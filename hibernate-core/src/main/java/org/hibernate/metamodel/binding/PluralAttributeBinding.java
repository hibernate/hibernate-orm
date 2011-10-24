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
package org.hibernate.metamodel.binding;

import java.util.Comparator;

import org.hibernate.metamodel.domain.PluralAttribute;
import org.hibernate.metamodel.relational.TableSpecification;
import org.hibernate.persister.collection.CollectionPersister;

/**
 * @author Steve Ebersole
 */
public interface PluralAttributeBinding extends  AssociationAttributeBinding {
	// todo : really it is the element (and/or index) that can be associative not the collection itself...

	@Override
	public PluralAttribute getAttribute();

	public CollectionKey getCollectionKey();

	public AbstractCollectionElement getCollectionElement();

	public TableSpecification getCollectionTable();

	public boolean isMutable();

	public Caching getCaching();

	public Class<? extends CollectionPersister> getCollectionPersisterClass();

	public String getCustomLoaderName();

	public CustomSQL getCustomSqlInsert();

	public CustomSQL getCustomSqlUpdate();

	public CustomSQL getCustomSqlDelete();

	public CustomSQL getCustomSqlDeleteAll();

	public boolean isOrphanDelete();

	String getWhere();

	boolean isSorted();

	Comparator getComparator();

	int getBatchSize();

	java.util.Map getFilterMap();

	boolean isInverse();

	String getOrderBy();
}
