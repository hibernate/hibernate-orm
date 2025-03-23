/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
