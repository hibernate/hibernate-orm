/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
