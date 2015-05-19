/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
