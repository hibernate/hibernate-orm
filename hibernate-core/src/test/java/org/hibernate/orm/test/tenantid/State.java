/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
