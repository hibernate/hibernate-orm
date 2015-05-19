/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.query.internal.property;

import org.hibernate.envers.boot.internal.EnversService;

/**
 * Used for specifying restrictions on the revision number, corresponding to an audit entity.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class RevisionNumberPropertyName implements PropertyNameGetter {
	@Override
	public String get(EnversService enversService) {
		return enversService.getAuditEntitiesConfiguration().getRevisionNumberPath();
	}
}
