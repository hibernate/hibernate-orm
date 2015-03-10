/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.Caching;
import org.hibernate.boot.model.CustomSql;

/**
 * @author Steve Ebersole
 */
public interface PluralAttributeSource
		extends AttributeSource,
				FetchableAttributeSource,
				CascadeStyleSource {

	public PluralAttributeNature getNature();

	public CollectionIdSource getCollectionIdSource();

	public PluralAttributeKeySource getKeySource();

	public PluralAttributeElementSource getElementSource();

	public FilterSource[] getFilterSources();

	public TableSpecificationSource getCollectionTableSpecificationSource();

	public String getCollectionTableComment();

	public String getCollectionTableCheck();

	/**
	 * Obtain any additional table names on which to synchronize (auto flushing) this entity.
	 *
	 * @return Additional synchronized table names or 0 sized String array, never return null.
	 */
	public String[] getSynchronizedTableNames();

	public Caching getCaching();

	public String getCustomPersisterClassName();

	public String getWhere();

	public boolean isInverse();

	public boolean isMutable();

	public String getCustomLoaderName();

	public CustomSql getCustomSqlInsert();

	public CustomSql getCustomSqlUpdate();

	public CustomSql getCustomSqlDelete();

	public CustomSql getCustomSqlDeleteAll();

	public String getMappedBy();

	public boolean usesJoinTable();

	@Override
	FetchCharacteristicsPluralAttribute getFetchCharacteristics();
}
