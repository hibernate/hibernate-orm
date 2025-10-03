/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;


import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;

import java.util.List;

/**
 * MultiFindOption implementation to specify whether the returned list
 * of entity instances should contain instances that have been
 * {@linkplain Session#remove(Object) marked for removal} in the
 * current session, but not yet deleted in the database.
 * <p>
 * The default is {@link #EXCLUDE}, meaning that instances marked for
 * removal are replaced by null in the returned list of entities when {@link OrderedReturn}
 * is used.
 *
 * @see org.hibernate.MultiFindOption
 * @see OrderedReturn
 * @see org.hibernate.Session#findMultiple(Class, List, FindOption...)
 * @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
 */
public enum IncludeRemovals implements MultiFindOption {
	INCLUDE,
	EXCLUDE
}
