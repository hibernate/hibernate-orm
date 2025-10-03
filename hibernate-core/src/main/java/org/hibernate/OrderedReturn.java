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
 * of entity instances should be ordered, where the position of an entity
 * instance is determined by the position of its identifier
 * in the list of ids passed to {@code findMultiple(...)}.
 * <p>
 * The default is {@link #ORDERED}, meaning the positions of the entities
 * in the returned list correspond to the positions of their ids. In this case,
 * the {@link IncludeRemovals} handling of entities marked for removal
 * becomes important.
 *
 * @see org.hibernate.MultiFindOption
 * @see IncludeRemovals
 * @see org.hibernate.Session#findMultiple(Class, List, FindOption...)
 * @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
 */
public enum OrderedReturn implements MultiFindOption {
	ORDERED,
	UNORDERED
}
