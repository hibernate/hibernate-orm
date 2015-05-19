/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.procedure;

/**
 * Defines support for executing database stored procedures and functions and accessing its outputs.
 * <p/>
 * First a reference to  {@link ProcedureCall} is obtained through one of the overloaded
 * {@link org.hibernate.Session#createStoredProcedureCall} methods.  The ProcedureCall reference is then used to "configure"
 * the procedure call (set timeouts, etc) and to perform parameter registration.  All procedure parameters that the
 * application wants to use must be registered.  For all IN and INOUT parameters, values can then be bound.
 * <p/>
 * At this point we are ready to execute the procedure call and start accessing the outputs.  This is done by first
 * calling the {@link ProcedureCall#getResult()} method.  The underlying JDBC call is executed as needed.  The pattern to
 * access the returns is iterating through the outputs while {@link ProcedureResult#hasMoreReturns()} returns {@code true} and
 * calling {@link ProcedureResult#getNextReturn()} during iteration:
 * <code>
 *     ProcedureCall call = session.createStoredProcedureCall( "some_procedure" );
 *     ...
 *     ProcedureResult result = call.getResult();
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
 * <p/>
 * Finally output parameters can be accessed using the overloaded {@link ProcedureResult#getOutputParameterValue} methods.
 * For portability amongst databases, it is advised to access the output parameters after all returns have been
 * processed.
 *
 * @see org.hibernate.Session#createStoredProcedureCall(String)
 * @see org.hibernate.Session#createStoredProcedureCall(String, Class[])
 * @see org.hibernate.Session#createStoredProcedureCall(String, String...)
 */
