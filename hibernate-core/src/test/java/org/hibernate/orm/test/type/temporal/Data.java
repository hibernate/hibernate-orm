/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.temporal;

/**
 * @author Steve Ebersole
 */
public interface Data<V> {
	V makeValue();
}
