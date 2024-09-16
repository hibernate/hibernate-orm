/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.tenantid;

import jakarta.persistence.Embeddable;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Embeddable
public class State {
	public boolean deleted;
	public @TenantId String tenantId;
	public @UpdateTimestamp Instant updated;
}
