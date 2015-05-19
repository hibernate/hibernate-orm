/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
