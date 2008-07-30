/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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
