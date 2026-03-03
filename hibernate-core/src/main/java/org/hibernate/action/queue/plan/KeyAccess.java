/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface KeyAccess {
	Object getKeyValue();
}
