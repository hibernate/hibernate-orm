/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * @since 7.0
 */
@Incubating
public interface JpaFunctionRoot<E> extends JpaFunctionFrom<E, E>, JpaRoot<E> {

}
