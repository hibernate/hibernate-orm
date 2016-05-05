/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.naming;

import java.io.Serializable;

import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Common contract for all implicit naming sources
 *
 * @author Steve Ebersole
 */
public interface ImplicitNameSource extends Serializable {
	/**
	 * Access to the current building context.
	 *
	 * @return The building context
	 */
	MetadataBuildingContext getBuildingContext();
}
