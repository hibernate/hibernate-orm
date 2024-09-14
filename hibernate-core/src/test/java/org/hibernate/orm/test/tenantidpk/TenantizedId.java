/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tenantidpk;

import java.util.UUID;

import jakarta.persistence.Embeddable;

@Embeddable
public class TenantizedId {
	Long id;
	UUID tenantId;

	public TenantizedId(Long id, UUID tenantId) {
		this.id = id;
		this.tenantId = tenantId;
	}

	TenantizedId() {}
}
