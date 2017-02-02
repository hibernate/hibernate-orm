/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.jaxb.hbm.spi;

import java.util.List;

/**
 * Common interface for all entity mappings (root entity and sub-entity mappings).
 *
 * @author Steve Ebersole
 */
public interface EntityInfo extends ToolingHintContainer {
	String getName();

	String getEntityName();

	Boolean isAbstract();

	Boolean isLazy();

	String getProxy();

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
