/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.HibernateException;
import org.hibernate.sql.results.graph.FetchOptions;

/**
 * Details about the discriminator for an embeddable hierarchy.
 *
 * @author Marco Belladelli
 * @see EmbeddableMappingType#getDiscriminatorMapping()
 */
public interface EmbeddableDiscriminatorMapping extends DiscriminatorMapping, FetchOptions {
	/**
	 * Retrieve the relational discriminator value corresponding to the provided embeddable class name.
	 *
	 * @throws HibernateException if the embeddable class name is not handled by this discriminator
	 */
	default Object getDiscriminatorValue(String embeddableClassName) {
		return getValueConverter().getDetailsForEntityName( embeddableClassName ).getValue();
	}
}
