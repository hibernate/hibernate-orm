/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;
import java.util.Date;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class LastUpdateListener {
	@PreUpdate
	@PrePersist
	public void setLastUpdate(Cat o) {
		o.setLastUpdate( new Date() );
		o.setManualVersion( o.getManualVersion() + 1 );
	}
}
