/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * This package defines a framework which models events occurring
 * within a stateful Hibernate {@link org.hibernate.Session}. An
 * {@linkplain org.hibernate.event.spi.AbstractSessionEvent event}
 * represents a request by the session API for some work to be
 * performed, and an event listener must respond to the event and
 * do that work, usually by scheduling some sort of
 * {@linkplain org.hibernate.action.spi.Executable action}.
 *
 * @see org.hibernate.action
 */
package org.hibernate.event;
