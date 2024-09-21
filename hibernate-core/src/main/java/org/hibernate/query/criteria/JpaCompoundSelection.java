/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.CompoundSelection;

/**
 * @author Steve Ebersole
 */
public interface JpaCompoundSelection<T> extends JpaSelection<T>, CompoundSelection<T> {
}
