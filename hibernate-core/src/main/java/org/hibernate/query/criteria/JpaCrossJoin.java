/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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
