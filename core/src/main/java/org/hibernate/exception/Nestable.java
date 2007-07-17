/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2002-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowledgement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowledgements normally appear.
 *
 * 4. The names "The Jakarta Project", "Commons", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */
package org.hibernate.exception;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * An interface to be implemented by {@link java.lang.Throwable}
 * extensions which would like to be able to nest root exceptions
 * inside themselves.
 *
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:knielsen@apache.org">Kasper Nielsen</a>
 * @author <a href="mailto:steven@caswell.name">Steven Caswell</a>
 * @author Pete Gieser
 * @version $Id: Nestable.java 4782 2004-11-21 00:11:27Z pgmjsd $
 * @since 1.0
 */
public interface Nestable {

	/**
	 * Returns the reference to the exception or error that caused the
	 * exception implementing the <code>Nestable</code> to be thrown.
	 *
	 * @return throwable that caused the original exception
	 */
	public Throwable getCause();

	/**
	 * Returns the error message of this and any nested
	 * <code>Throwable</code>.
	 *
	 * @return the error message
	 */
	public String getMessage();

	/**
	 * Returns the error message of the <code>Throwable</code> in the chain
	 * of <code>Throwable</code>s at the specified index, numbererd from 0.
	 *
	 * @param index the index of the <code>Throwable</code> in the chain of
	 *              <code>Throwable</code>s
	 * @return the error message, or null if the <code>Throwable</code> at the
	 *         specified index in the chain does not contain a message
	 * @throws IndexOutOfBoundsException if the <code>index</code> argument is
	 *                                   negative or not less than the count of <code>Throwable</code>s in the
	 *                                   chain
	 */
	public String getMessage(int index);

	/**
	 * Returns the error message of this and any nested <code>Throwable</code>s
	 * in an array of Strings, one element for each message. Any
	 * <code>Throwable</code> not containing a message is represented in the
	 * array by a null. This has the effect of cause the length of the returned
	 * array to be equal to the result of the {@link #getThrowableCount()}
	 * operation.
	 *
	 * @return the error messages
	 */
	public String[] getMessages();

	/**
	 * Returns the <code>Throwable</code> in the chain of
	 * <code>Throwable</code>s at the specified index, numbererd from 0.
	 *
	 * @param index the index, numbered from 0, of the <code>Throwable</code> in
	 *              the chain of <code>Throwable</code>s
	 * @return the <code>Throwable</code>
	 * @throws IndexOutOfBoundsException if the <code>index</code> argument is
	 *                                   negative or not less than the count of <code>Throwable</code>s in the
	 *                                   chain
	 */
	public Throwable getThrowable(int index);

	/**
	 * Returns the number of nested <code>Throwable</code>s represented by
	 * this <code>Nestable</code>, including this <code>Nestable</code>.
	 *
	 * @return the throwable count
	 */
	public int getThrowableCount();

	/**
	 * Returns this <code>Nestable</code> and any nested <code>Throwable</code>s
	 * in an array of <code>Throwable</code>s, one element for each
	 * <code>Throwable</code>.
	 *
	 * @return the <code>Throwable</code>s
	 */
	public Throwable[] getThrowables();

	/**
	 * Returns the index, numbered from 0, of the first occurrence of the
	 * specified type in the chain of <code>Throwable</code>s, or -1 if the
	 * specified type is not found in the chain.
	 *
	 * @param type <code>Class</code> to be found
	 * @return index of the first occurrence of the type in the chain, or -1 if
	 *         the type is not found
	 */
	public int indexOfThrowable(Class type);

	/**
	 * Returns the index, numbered from 0, of the first <code>Throwable</code>
	 * that matches the specified type in the chain of <code>Throwable</code>s
	 * with an index greater than or equal to the specified index, or -1 if
	 * the type is not found.
	 *
	 * @param type      <code>Class</code> to be found
	 * @param fromIndex the index, numbered from 0, of the starting position in
	 *                  the chain to be searched
	 * @return index of the first occurrence of the type in the chain, or -1 if
	 *         the type is not found
	 * @throws IndexOutOfBoundsException if the <code>fromIndex</code> argument
	 *                                   is negative or not less than the count of <code>Throwable</code>s in the
	 *                                   chain
	 */
	public int indexOfThrowable(Class type, int fromIndex);

	/**
	 * Prints the stack trace of this exception to the specified print
	 * writer.  Includes information from the exception, if any,
	 * which caused this exception.
	 *
	 * @param out <code>PrintWriter</code> to use for output.
	 */
	public void printStackTrace(PrintWriter out);

	/**
	 * Prints the stack trace of this exception to the specified print
	 * stream.  Includes inforamation from the exception, if any,
	 * which caused this exception.
	 *
	 * @param out <code>PrintStream</code> to use for output.
	 */
	public void printStackTrace(PrintStream out);

	/**
	 * Prints the stack trace for this exception only--root cause not
	 * included--using the provided writer.  Used by {@link
	 * org.apache.commons.lang.exception.NestableDelegate} to write
	 * individual stack traces to a buffer.  The implementation of
	 * this method should call
	 * <code>super.printStackTrace(out);</code> in most cases.
	 *
	 * @param out The writer to use.
	 */
	public void printPartialStackTrace(PrintWriter out);

}
