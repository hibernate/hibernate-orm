/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import org.hibernate.Incubating;

/**
 * @author Christian Beikov
 */
@Incubating
public interface JpaDerivedRoot<T> extends JpaDerivedFrom<T>, JpaRoot<T> {

}
