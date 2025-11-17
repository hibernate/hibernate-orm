/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines support for executing database stored procedures and functions and accessing their outputs.
 * <p>
 * This API is used as follows:
 * <ol>
 * <li>First a reference to {@link org.hibernate.procedure.ProcedureCall} is obtained via one of the
 *     overloaded {@link org.hibernate.Session#createStoredProcedureCall} methods.
 * <li>The {@code ProcedureCall} reference is then used to "configure" the procedure call
 *     (set timeouts and such) and to perform parameter registration.
 *     All procedure parameters that the application wants to use must be registered.
 *     For each IN or INOUT parameter, a value may then be bound.
 * <li>At this point we are ready to execute the procedure call and start accessing the outputs.
 *    This is done by first calling the {@link org.hibernate.procedure.ProcedureCall#getOutputs()}}
 *    method. The underlying JDBC call is executed as needed. The pattern to access the returns is
 *    iteration over the outputs while {@link org.hibernate.procedure.ProcedureOutputs#goToNext()}}
 *    returns {@code true} and calling {@link org.hibernate.procedure.ProcedureOutputs#getCurrent()}}
 *    during iteration:
 *     <pre>
 *     ProcedureCall call = session.createStoredProcedureCall( "some_procedure" );
 *     ...
 *     ProcedureOutputs outputs = call.getOutputs();
 *     while ( outputs.goToNext() ) {
 *         final Output output = outputs.getCurrent();
 *         if ( output.isResultSet() ) {
 *             handleResultSetOutput( (ResultSetOutput) output );
 *         }
 *         else {
 *             handleUpdateCountOutput( (UpdateCountOutput) output );
 *         }
 *     }
 *     </pre>
 * <li>Finally, output parameters can be accessed using the overloaded
 *     {@link org.hibernate.procedure.ProcedureOutputs#getOutputParameterValue} methods.
 *     For portability amongst databases, it is advised to access the output parameters after all
 *     returns have been processed.
 * </ol>
 *
 * @see org.hibernate.Session#createStoredProcedureCall(String)
 * @see org.hibernate.Session#createStoredProcedureCall(String, Class[])
 * @see org.hibernate.Session#createStoredProcedureCall(String, String...)
 */
package org.hibernate.procedure;
