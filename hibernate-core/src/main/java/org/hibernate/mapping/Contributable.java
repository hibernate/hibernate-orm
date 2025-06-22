/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import org.hibernate.boot.model.relational.ContributableDatabaseObject;

/**
 * Parts of the mapping model which are associated with a
 * {@linkplain #getContributor() contributor} (ORM, Envers, etc).
 * <p/>
 * The most useful aspect of this is the {@link ContributableDatabaseObject}
 * specialization.
 *
 * @author Steve Ebersole
 */
public interface Contributable {
	/**
	 * The name of the contributor which contributed this
	 */
	String getContributor();
}
