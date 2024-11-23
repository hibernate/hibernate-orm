/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * An SPI for defining, registering, and rendering functions in HQL. The
 * {@link org.hibernate.query.sqm.function.SqmFunctionRegistry} maintains
 * a list of {@linkplain org.hibernate.query.sqm.function.SqmFunctionDescriptor
 * function descriptors}. User-written code may contribute function descriptors
 * by calling {@link org.hibernate.cfg.Configuration#addSqlFunction} or by
 * registering a {@link org.hibernate.boot.model.FunctionContributor}.
 *
 * @see org.hibernate.query.sqm.function.SqmFunctionDescriptor
 * @see org.hibernate.query.sqm.function.SqmFunctionRegistry
 * @see org.hibernate.cfg.Configuration#addSqlFunction
 * @see org.hibernate.boot.MetadataBuilder#applySqlFunction
 * @see org.hibernate.boot.model.FunctionContributor
 * @see org.hibernate.query.spi.QueryEngine#getSqmFunctionRegistry()
 */
package org.hibernate.query.sqm.function;
