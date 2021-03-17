/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines SPI hooks into the {@link org.hibernate.engine.spi.ActionQueue}.  Mainly for registering custom
 * {@link org.hibernate.action.spi.AfterTransactionCompletionProcess} and {@link org.hibernate.action.spi.BeforeTransactionCompletionProcess} hooks.
 */
package org.hibernate.action.spi;
