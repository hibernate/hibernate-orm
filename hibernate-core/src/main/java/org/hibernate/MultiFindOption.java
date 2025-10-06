/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;


import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;

import java.util.List;

/**
 * Simple marker interface for FindOptions which can be applied to multiple id loading.
 *
 * @see org.hibernate.Session#findMultiple(Class, List, FindOption...)
 * @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
 *
 * @since 7.2
 */
public interface MultiFindOption extends FindOption {
}
