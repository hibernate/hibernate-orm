/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * This package defines a framework which models events occurring
 * within a stateful Hibernate {@link org.hibernate.Session}. An
 * {@linkplain org.hibernate.event.spi.AbstractEvent event}
 * represents a request by the session API for some work to be
 * performed, and an event listener must respond to the event and
 * do that work, usually by scheduling some sort of
 * {@linkplain org.hibernate.action.spi.Executable action}.
 *
 * @see org.hibernate.action
 */
package org.hibernate.event;
