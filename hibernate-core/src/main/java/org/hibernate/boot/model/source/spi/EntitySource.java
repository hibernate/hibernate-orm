/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedNativeQueryType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNamedQueryType;
import org.hibernate.boot.model.CustomSql;
import org.hibernate.boot.model.TruthValue;

/**
 * Contract describing source of information related to mapping an entity.
 *
 * @author Steve Ebersole
 */
public interface EntitySource extends IdentifiableTypeSource, ToolingHintContextContainer, EntityNamingSourceContributor {
	/**
	 * Obtain the primary table for this entity.
	 *
	 * @return The primary table.
	 */
	TableSpecificationSource getPrimaryTable();

	/**
	 * Obtain the secondary tables for this entity
	 *
	 * @return returns an iterator over the secondary tables for this entity
	 */
	Map<String,SecondaryTableSource> getSecondaryTableMap();

	String getXmlNodeName();

	/**
	 * Obtain the named custom tuplizer classes to be used.
	 *
	 * @return The custom tuplizer class names
	 */
	Map<EntityMode,String> getTuplizerClassMap();

	/**
	 * Obtain the name of a custom persister class to be used.
	 *
	 * @return The custom persister class name
	 */
	String getCustomPersisterClassName();

	/**
	 * Is this entity lazy (proxyable)?
	 *
	 * @return {@code true} indicates the entity is lazy; {@code false} non-lazy.
	 */
	boolean isLazy();

	/**
	 * For {@link #isLazy() lazy} entities, obtain the interface to use in constructing its proxies.
	 *
	 * @return The proxy interface name
	 */
	String getProxy();

	/**
	 * Obtain the batch-size to be applied when initializing proxies of this entity.
	 *
	 * @return returns the the batch-size.
	 */
	int getBatchSize();

	/**
	 * Is the entity abstract?
	 * <p/>
	 * The implication is whether the entity maps to a database table.
	 *
	 * @return {@code true} indicates the entity is abstract; {@code false} non-abstract; {@code null}
	 * indicates that a reflection check should be done when building the persister.
	 */
	Boolean isAbstract();

	/**
	 * Did the source specify dynamic inserts?
	 *
	 * @return {@code true} indicates dynamic inserts will be used; {@code false} otherwise.
	 */
	boolean isDynamicInsert();

	/**
	 * Did the source specify dynamic updates?
	 *
	 * @return {@code true} indicates dynamic updates will be used; {@code false} otherwise.
	 */
	boolean isDynamicUpdate();

	/**
	 * Did the source specify to perform selects to decide whether to perform (detached) updates?
	 *
	 * @return {@code true} indicates selects will be done; {@code false} otherwise.
	 */
	boolean isSelectBeforeUpdate();

	/**
	 * Obtain the name of a named-query that will be used for loading this entity
	 *
	 * @return THe custom loader query name
	 */
	String getCustomLoaderName();

	/**
	 * Obtain the custom SQL to be used for inserts for this entity
	 *
	 * @return The custom insert SQL
	 */
	CustomSql getCustomSqlInsert();

	/**
	 * Obtain the custom SQL to be used for updates for this entity
	 *
	 * @return The custom update SQL
	 */
	CustomSql getCustomSqlUpdate();

	/**
	 * Obtain the custom SQL to be used for deletes for this entity
	 *
	 * @return The custom delete SQL
	 */
	CustomSql getCustomSqlDelete();

	/**
	 * Obtain any additional table names on which to synchronize (auto flushing) this entity.
	 *
	 * @return Additional synchronized table names or 0 sized String array, never return null.
	 */
	String[] getSynchronizedTableNames();

	/**
	 * Get the actual discriminator value in case of a single table inheritance
	 *
	 * @return the actual discriminator value in case of a single table inheritance or {@code null} in case there is no
	 *         explicit value or a different inheritance scheme
	 */
	String getDiscriminatorMatchValue();

	/**
	 * Obtain the filters for this entity.
	 *
	 * @return returns an array of the filters for this entity.
	 */
	FilterSource[] getFilterSources();

	List<JaxbHbmNamedQueryType> getNamedQueries();

	List<JaxbHbmNamedNativeQueryType> getNamedNativeQueries();

	TruthValue quoteIdentifiersLocalToEntity();

}
