/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

/**
 * Context for determining the implicit name of an entity's tenant identifier
 * column.
 *
 * @author Steve Ebersole
 */
public interface ImplicitTenantIdColumnNameSource extends ImplicitNameSource {
	/**
	 * Access the entity name information
	 *
	 * @return The entity name information
	 */
	EntityNaming getEntityNaming();
}
