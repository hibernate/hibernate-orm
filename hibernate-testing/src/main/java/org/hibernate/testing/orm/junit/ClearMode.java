/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.Incubating;

@Incubating
public enum ClearMode {
	BEFORE_EACH, AFTER_EACH, AFTER_ALL, NEVER
}
