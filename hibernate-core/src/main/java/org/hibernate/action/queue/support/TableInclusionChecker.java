/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.support;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.TableDescriptor;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface TableInclusionChecker {
	boolean include(TableDescriptor tableDescriptor);
}
