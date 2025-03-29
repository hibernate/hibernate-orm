/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * Commonality between the various forms of plural attribute (collection) mappings: {@code <bag/>}, {@code <set/>}, etc.
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeInfo extends AttributeMapping, TableInformationContainer, ToolingHintContainer {
	JaxbHbmKeyType getKey();

	JaxbHbmBasicCollectionElementType getElement();

	JaxbHbmCompositeCollectionElementType getCompositeElement();

	JaxbHbmOneToManyCollectionElementType getOneToMany();

	JaxbHbmManyToManyCollectionElementType getManyToMany();

	JaxbHbmManyToAnyCollectionElementType getManyToAny();

	String getComment();

	String getCheck();

	String getWhere();

	JaxbHbmLoaderType getLoader();

	JaxbHbmCustomSqlDmlType getSqlInsert();

	JaxbHbmCustomSqlDmlType getSqlUpdate();

	JaxbHbmCustomSqlDmlType getSqlDelete();

	JaxbHbmCustomSqlDmlType getSqlDeleteAll();

	List<JaxbHbmSynchronizeType> getSynchronize();

	JaxbHbmCacheType getCache();

	List<JaxbHbmFilterType> getFilter();

	String getCascade();

	JaxbHbmFetchStyleWithSubselectEnum getFetch();

	JaxbHbmLazyWithExtraEnum getLazy();

	JaxbHbmOuterJoinEnum getOuterJoin();

	int getBatchSize();

	boolean isInverse();

	boolean isMutable();

	boolean isOptimisticLock();

	String getCollectionType();

	String getPersister();

// todo : not available on all.  do we need a specific interface for these?
//	public String getSort();
//	public String getOrderBy();
}
