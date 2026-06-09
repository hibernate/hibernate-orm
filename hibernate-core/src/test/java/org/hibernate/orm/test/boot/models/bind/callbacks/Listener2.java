/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.bind.callbacks;

import jakarta.persistence.PostLoad;

/**
 * @author Steve Ebersole
 */
public class Listener2 {
	@PostLoad
	public void wasLoaded(Object entity) {
	}
}
