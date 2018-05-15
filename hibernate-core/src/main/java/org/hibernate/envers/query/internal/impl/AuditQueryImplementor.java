/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.impl;

import org.hibernate.Incubating;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.projection.AuditProjection;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
@Incubating(since = "5.2")
interface AuditQueryImplementor extends AuditQuery {
	/**
	 * Register an entity audit projection.
	 *
	 * @param entityName The entity name
	 * @param projection The projection to apply for entity
	 */
	void registerProjection(final String entityName, final AuditProjection projection);
}
