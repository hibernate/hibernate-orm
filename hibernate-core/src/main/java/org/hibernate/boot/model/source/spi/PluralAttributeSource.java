/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.CustomSql;

/**
 * @author Steve Ebersole
 */
public interface PluralAttributeSource
		extends AttributeSource,
				FetchableAttributeSource,
				CascadeStyleSource {

	PluralAttributeNature getNature();

	CollectionIdSource getCollectionIdSource();

	PluralAttributeKeySource getKeySource();

	PluralAttributeElementSource getElementSource();

	FilterSource[] getFilterSources();

	TableSpecificationSource getCollectionTableSpecificationSource();

	String getCollectionTableComment();

	String getCollectionTableCheck();

	/**
	 * Obtain any additional table names on which to synchronize (auto flushing) this entity.
	 *
	 * @return Additional synchronized table names or 0 sized String array, never return null.
	 */
	String[] getSynchronizedTableNames();

	Caching getCaching();

	String getCustomPersisterClassName();

	String getWhere();

	boolean isInverse();

	boolean isMutable();

	String getCustomLoaderName();

	CustomSql getCustomSqlInsert();

	CustomSql getCustomSqlUpdate();

	CustomSql getCustomSqlDelete();

	CustomSql getCustomSqlDeleteAll();

	String getMappedBy();

	boolean usesJoinTable();

	@Override
	FetchCharacteristicsPluralAttribute getFetchCharacteristics();
}
