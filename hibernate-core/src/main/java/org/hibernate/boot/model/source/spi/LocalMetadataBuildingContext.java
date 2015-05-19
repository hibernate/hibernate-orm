/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Specialization of the MetadataBuildingContext contract specific to a given origin.
 *
 * @author Steve Ebersole
 */
public interface LocalMetadataBuildingContext extends MetadataBuildingContext {
	/**
	 * Obtain the origin for this context
	 *
	 * @return The origin
	 */
	public Origin getOrigin();
}
