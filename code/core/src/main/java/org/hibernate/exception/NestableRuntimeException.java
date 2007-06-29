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
import java.io.ObjectOutputStream;
import java.io.IOException;

import antlr.RecognitionException;

/**
 * The base class of all runtime exceptions which can contain other
 * exceptions.
 *
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:knielsen@apache.org">Kasper Nielsen</a>
 * @author <a href="mailto:steven@caswell.name">Steven Caswell</a>
 * @version $Id: NestableRuntimeException.java 8137 2005-09-09 15:21:10Z epbernard $
 * @see org.apache.commons.lang.exception.NestableException
 * @since 1.0
 */
public class NestableRuntimeException extends RuntimeException implements Nestable {

	/**
	 * The helper instance which contains much of the code which we
	 * delegate to.
	 */
	protected NestableDelegate delegate = new NestableDelegate( this );

	/**
	 * Holds the reference to the exception or error that caused
	 * this exception to be thrown.
	 */
	private Throwable cause = null;

	/**
	 * Constructs a new <code>NestableRuntimeException</code> without specified
	 * detail message.
	 */
	public NestableRuntimeException() {
		super();
	}

	/**
	 * Constructs a new <code>NestableRuntimeException</code> with specified
	 * detail message.
	 *
	 * @param msg the error message
	 */
	public NestableRuntimeException(String msg) {
		super( msg );
	}

	/**
	 * Constructs a new <code>NestableRuntimeException</code> with specified
	 * nested <code>Throwable</code>.
	 *
	 * @param cause the exception or error that caused this exception to be
	 *              thrown
	 */
	public NestableRuntimeException(Throwable cause) {
		super();
		this.cause = cause;
	}

	/**
	 * Constructs a new <code>NestableRuntimeException</code> with specified
	 * detail message and nested <code>Throwable</code>.
	 *
	 * @param msg   the error message
	 * @param cause the exception or error that caused this exception to be
	 *              thrown
	 */
	public NestableRuntimeException(String msg, Throwable cause) {
		super( msg );
		this.cause = cause;
	}

	public Throwable getCause() {
		return cause;
	}

	/**
	 * Returns the detail message string of this throwable. If it was
	 * created with a null message, returns the following:
	 * ( cause==null ? null : cause.toString( ).
	 */
	public String getMessage() {
		if ( super.getMessage() != null ) {
			return super.getMessage();
		}
		else if ( cause != null ) {
			return cause.toString();
		}
		else {
			return null;
		}
	}

	public String getMessage(int index) {
		if ( index == 0 ) {
			return super.getMessage();
		}
		else {
			return delegate.getMessage( index );
		}
	}

	public String[] getMessages() {
		return delegate.getMessages();
	}

	public Throwable getThrowable(int index) {
		return delegate.getThrowable( index );
	}

	public int getThrowableCount() {
		return delegate.getThrowableCount();
	}

	public Throwable[] getThrowables() {
		return delegate.getThrowables();
	}

	public int indexOfThrowable(Class type) {
		return delegate.indexOfThrowable( type, 0 );
	}

	public int indexOfThrowable(Class type, int fromIndex) {
		return delegate.indexOfThrowable( type, fromIndex );
	}

	public void printStackTrace() {
		delegate.printStackTrace();
	}

	public void printStackTrace(PrintStream out) {
		delegate.printStackTrace( out );
	}

	public void printStackTrace(PrintWriter out) {
		delegate.printStackTrace( out );
	}

	public final void printPartialStackTrace(PrintWriter out) {
		super.printStackTrace( out );
	}



	private void writeObject(ObjectOutputStream oos) throws IOException {
		Throwable tempCause = cause;
		//don't propagate RecognitionException, might be not serializable
		if ( cause instanceof RecognitionException ) {
			cause = null;
		}
		oos.defaultWriteObject();
		cause = tempCause;
	}

}
