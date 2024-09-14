/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

/**
 * Model part which can be soft-deleted
 *
 * @author Steve Ebersole
 */
public interface SoftDeletableModelPart extends ModelPartContainer {
	/**
	 * Get the mapping of the soft-delete indicator
	 */
	SoftDeleteMapping getSoftDeleteMapping();

	TableDetails getSoftDeleteTableDetails();
}
