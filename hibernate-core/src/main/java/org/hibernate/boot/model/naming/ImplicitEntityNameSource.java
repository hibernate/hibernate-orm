/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.naming;

/**
 * Context for determining the implicit name of an entity's primary table
 *
 * @author Steve Ebersole
 */
public interface ImplicitEntityNameSource extends ImplicitNameSource {
	/**
	 * Access to the entity's name information
	 *
	 * @return The entity's name information
	 */
	EntityNaming getEntityNaming();
}
