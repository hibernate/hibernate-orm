/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines the event types and event listener interfaces for
 * events produced by the stateful  {@link org.hibernate.Session}.
 * <p>
 * An {@linkplain org.hibernate.event.spi.AbstractEvent event}
 * represents a request by the session API for some work to be
 * performed, and an event listener must respond to the event and
 * do that work, usually by scheduling some sort of
 * {@linkplain org.hibernate.action.spi.Executable action}.
 */
package org.hibernate.event.spi;
