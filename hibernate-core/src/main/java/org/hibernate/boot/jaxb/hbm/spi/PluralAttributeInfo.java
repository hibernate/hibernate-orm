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
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * Commonality between the various forms of plural attribute (collection) mappings: {@code <bag/>}, {@code <set/>}, etc.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeInfo extends AttributeMapping, TableInformationContainer, ToolingHintContainer {
	public JaxbHbmKeyType getKey();

	public JaxbHbmBasicCollectionElementType getElement();
	public JaxbHbmCompositeCollectionElementType getCompositeElement();
	public JaxbHbmOneToManyCollectionElementType getOneToMany();
	public JaxbHbmManyToManyCollectionElementType getManyToMany();
    public JaxbHbmManyToAnyCollectionElementType getManyToAny();

	public String getComment();
	public String getCheck();
	public String getWhere();

	public JaxbHbmLoaderType getLoader();
	public JaxbHbmCustomSqlDmlType getSqlInsert();
    public JaxbHbmCustomSqlDmlType getSqlUpdate();
    public JaxbHbmCustomSqlDmlType getSqlDelete();
    public JaxbHbmCustomSqlDmlType getSqlDeleteAll();

	public List<JaxbHbmSynchronizeType> getSynchronize();

	public JaxbHbmCacheType getCache();
	public List<JaxbHbmFilterType> getFilter();

	public String getCascade();
	public JaxbHbmFetchStyleWithSubselectEnum getFetch();
	public JaxbHbmLazyWithExtraEnum getLazy();
	public JaxbHbmOuterJoinEnum getOuterJoin();

	public int getBatchSize();
	public boolean isInverse();
    public boolean isMutable();
	public boolean isOptimisticLock();

	public String getCollectionType();
    public String getPersister();

// todo : not available on all.  do we need a specific interface for these?
//	public String getSort();
//	public String getOrderBy();
}
