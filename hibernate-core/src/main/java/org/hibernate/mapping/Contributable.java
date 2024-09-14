/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
