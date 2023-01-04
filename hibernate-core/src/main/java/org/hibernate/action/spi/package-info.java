/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * This SPI package defines an abstraction over the notion of an "action"
 * which is scheduled for asynchronous execution by the event listeners.
 * Every action implements {@link org.hibernate.action.spi.Executable}.
 * <p>
 * The {@link org.hibernate.engine.spi.ActionQueue} is responsible for
 * scheduling and execution of the actions.
 * <p>
 * This package also defines the SPI callback interfaces for the
 * {@link org.hibernate.engine.spi.ActionQueue}, allowing registration of
 * custom {@link org.hibernate.action.spi.AfterTransactionCompletionProcess}
 * and {@link org.hibernate.action.spi.BeforeTransactionCompletionProcess}
 * processors.
 */
package org.hibernate.action.spi;
