/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * This package defines the notion of an "action" which is scheduled for
 * asynchronous execution by the event listeners. Every action implements
 * {@link org.hibernate.action.spi.Executable}.
 * <p>
 * The {@link org.hibernate.engine.spi.ActionQueue} is responsible for
 * scheduling and execution of the actions.
 *
 * @see org.hibernate.event
 */
package org.hibernate.action;
