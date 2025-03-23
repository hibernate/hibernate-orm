/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines support for dealing with results returned from database via JDBC.
 * <p>
 * Accounts for mixed result sets and update counts, hiding the complexity of how
 * this is exposed via the JDBC API.
 * <ul>
 * <li>{@link org.hibernate.result.Outputs} represents the overall group of results.
 * <li>{@link org.hibernate.result.Output} represents the mixed individual outcomes,
 *     which might be either a {@link org.hibernate.result.ResultSetOutput} or
 *     a {@link org.hibernate.result.UpdateCountOutput}.
 * </ul>
 * <p>
 * <pre>
 *     Outputs outputs = ...;
 *     while ( outputs.goToNext() ) {
 *         final Output output = outputs.getCurrent();
 *         if ( rtn.isResultSet() ) {
 *             handleResultSetOutput( (ResultSetOutput) output );
 *         }
 *         else {
 *             handleUpdateCountOutput( (UpdateCountOutput) output );
 *         }
 *     }
 * </pre>
 */
package org.hibernate.result;
