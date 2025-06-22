/*
 * SPDX-License-Identifier: Apache-2.0
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
