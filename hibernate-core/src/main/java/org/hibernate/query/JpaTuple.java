/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query;

import org.hibernate.Incubating;

import jakarta.persistence.Tuple;

/**
 * Hibernate extension to the Jakarta Persistence {@link Tuple}
 * contract
 *
 * @author Steve Ebersole
 */
@Incubating
public interface JpaTuple extends Tuple {
}
