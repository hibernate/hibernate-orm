package org.hibernate.result;

/**
 * Defines support for dealing with database results, accounting for mixed result sets and update counts hiding the
 * complexity (IMO) of how this is exposed in the JDBC API.
 *
 * {@link Result} represents the overall group of results.
 *
 * {@link Return} represents the mixed individual outcomes, which might be either a {@link ResultSetReturn} or
 * a {@link UpdateCountReturn}.
 *
 * <code>
 *     Result result = ...;
 *     while ( result.hasMoreReturns() ) {
 *         final Return rtn = result.getNextReturn();
 *         if ( rtn.isResultSet() ) {
 *             handleResultSetReturn( (ResultSetReturn) rtn );
 *         }
 *         else {
 *             handleUpdateCountReturn( (UpdateCountReturn) rtn );
 *         }
 *     }
 * </code>
 */
