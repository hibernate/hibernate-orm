/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import java.util.Map;

/**
 * @author Steve Ebersole
 */
public interface CollectionIdSource {
	/**
	 * Obtain source information about the column for the collection id.
	 *
	 * @return The collection id column info.
	 */
	public ColumnSource getColumnSource();

	/**
	 * Obtain information about the Hibernate type ({@link org.hibernate.type.Type}) for the collection id
	 *
	 * @return The Hibernate type information
	 */
	public HibernateTypeSource getTypeInformation();

	/**
	 * Obtain the name of the identifier value generator.
	 *
	 * @return The identifier value generator name
	 */
	public String getGeneratorName();

	/**
	 * @return The identifier generator configuration parameters
	 */
	public Map<String, String> getParameters();
}
