/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
