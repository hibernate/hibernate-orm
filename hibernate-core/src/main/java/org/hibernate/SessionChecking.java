/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;


import jakarta.persistence.EntityGraph;
import jakarta.persistence.FindOption;

import java.util.List;

/**
 * MultiFindOption implementation to specify whether the ids of managed entity instances already
 * cached in the current persistence context should be excluded.
 * from the list of ids sent to the database.
 * <p>
 * The default is {@link #DISABLED}, meaning all ids are included and sent to the database.
 *
 * Use {@link #ENABLED} to exclude already managed entity instance ids from
 * the list of ids sent to the database.
 *
 * @see org.hibernate.MultiFindOption
 * @see org.hibernate.Session#findMultiple(Class, List , FindOption...)
 * @see org.hibernate.Session#findMultiple(EntityGraph, List , FindOption...)
 */
public record SessionChecking(boolean enabled) implements MultiFindOption {
	public static SessionChecking ENABLED = new SessionChecking( true );
	public static SessionChecking DISABLED = new SessionChecking( false );
}
