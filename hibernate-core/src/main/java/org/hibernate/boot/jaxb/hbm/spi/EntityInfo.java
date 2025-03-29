/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * Common interface for all entity mappings (root entity and sub-entity mappings).
 *
 * @author Steve Ebersole
 */
public interface EntityInfo extends ToolingHintContainer, ResultSetMappingContainer {
	String getName();

	String getEntityName();

	String getProxy();

	Boolean isAbstract();

	Boolean isLazy();

	int getBatchSize();

	boolean isDynamicInsert();

	boolean isDynamicUpdate();

	boolean isSelectBeforeUpdate();

	List<JaxbHbmTuplizerType> getTuplizer();

	String getPersister();

	JaxbHbmLoaderType getLoader();

	JaxbHbmCustomSqlDmlType getSqlInsert();

	JaxbHbmCustomSqlDmlType getSqlUpdate();

	JaxbHbmCustomSqlDmlType getSqlDelete();

	List<JaxbHbmSynchronizeType> getSynchronize();

	List<JaxbHbmFetchProfileType> getFetchProfile();

	List<JaxbHbmResultSetMappingType> getResultset();

	List<JaxbHbmNamedNativeQueryType> getSqlQuery();

	List<JaxbHbmNamedQueryType> getQuery();

	List getAttributes();
}
