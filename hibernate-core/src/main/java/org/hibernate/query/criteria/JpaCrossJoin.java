/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * @author Marco Belladelli
 */
@Incubating
public interface JpaCrossJoin<T> extends JpaFrom<T,T> {
}
