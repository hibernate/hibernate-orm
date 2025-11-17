/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines the event types and event listener interfaces for
 * events produced by the stateful {@link org.hibernate.Session}.
 * <p>
 * An {@linkplain org.hibernate.event.spi.AbstractSessionEvent event}
 * represents a request by the session API for some work to be
 * performed, and an event listener must respond to the event and
 * do that work, usually by scheduling some sort of
 * {@linkplain org.hibernate.action.spi.Executable action}.
 * <p>
 * Note that a {@link org.hibernate.StatelessSession} does not
 * produce events and does not make use of this framework.
 *
 * @apiNote The framework for event notifications defined in this
 *          package is intended for use by extremely sophisticated
 *          libraries and frameworks which extend Hibernate, and
 *          by the internal implementation of Hibernate itself.
 *          <p>Regular application code should prefer the use of
 *          JPA-defined lifecycle callback methods, that is,
 *          {@link jakarta.persistence.PostPersist @PostPersist}
 *          and friends, or an implementation of the venerable
 *          {@link org.hibernate.Interceptor} interface.
 */
package org.hibernate.event.spi;
