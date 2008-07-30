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

/**
 * The base class of all exceptions which can contain other exceptions.
 * <p/>
 * It is intended to ease the debugging by carrying on the information
 * about the exception which was caught and provoked throwing the
 * current exception. Catching and rethrowing may occur multiple
 * times, and provided that all exceptions except the first one
 * are descendands of <code>NestedException</code>, when the
 * exception is finally printed out using any of the <code>
 * printStackTrace()</code> methods, the stacktrace will contain
 * the information about all exceptions thrown and caught on
 * the way.
 * <p> Running the following program
 * <p><blockquote><pre>
 *  1 import org.apache.commons.lang.exception.NestableException;
 *  2
 *  3 public class Test {
 *  4     public static void main( String[] args ) {
 *  5         try {
 *  6             a();
 *  7         } catch(Exception e) {
 *  8             e.printStackTrace();
 *  9         }
 * 10      }
 * 11
 * 12      public static void a() throws Exception {
 * 13          try {
 * 14              b();
 * 15          } catch(Exception e) {
 * 16              throw new NestableException("foo", e);
 * 17          }
 * 18      }
 * 19
 * 20      public static void b() throws Exception {
 * 21          try {
 * 22              c();
 * 23          } catch(Exception e) {
 * 24              throw new NestableException("bar", e);
 * 25          }
 * 26      }
 * 27
 * 28      public static void c() throws Exception {
 * 29          throw new Exception("baz");
 * 30      }
 * 31 }
 * </pre></blockquote>
 * <p>Yields the following stacktrace:
 * <p><blockquote><pre>
 * org.apache.commons.lang.exception.NestableException: foo
 *         at Test.a(Test.java:16)
 *         at Test.main(Test.java:6)
 * Caused by: org.apache.commons.lang.exception.NestableException: bar
 *         at Test.b(Test.java:24)
 *         at Test.a(Test.java:14)
 *         ... 1 more
 * Caused by: java.lang.Exception: baz
 *         at Test.c(Test.java:29)
 *         at Test.b(Test.java:22)
 *         ... 2 more
 * </pre></blockquote><br>
 *
 * @author <a href="mailto:Rafal.Krzewski@e-point.pl">Rafal Krzewski</a>
 * @author <a href="mailto:dlr@collab.net">Daniel Rall</a>
 * @author <a href="mailto:knielsen@apache.org">Kasper Nielsen</a>
 * @author <a href="mailto:steven@caswell.name">Steven Caswell</a>
 * @version $Id: NestableException.java 4782 2004-11-21 00:11:27Z pgmjsd $
 * @since 1.0
 */
public class NestableException extends Exception implements Nestable {

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
	 * Constructs a new <code>NestableException</code> without specified
	 * detail message.
	 */
	public NestableException() {
		super();
	}

	/**
	 * Constructs a new <code>NestableException</code> with specified
	 * detail message.
	 *
	 * @param msg The error message.
	 */
	public NestableException(String msg) {
		super( msg );
	}

	/**
	 * Constructs a new <code>NestableException</code> with specified
	 * nested <code>Throwable</code>.
	 *
	 * @param cause the exception or error that caused this exception to be
	 *              thrown
	 */
	public NestableException(Throwable cause) {
		super();
		this.cause = cause;
	}

	/**
	 * Constructs a new <code>NestableException</code> with specified
	 * detail message and nested <code>Throwable</code>.
	 *
	 * @param msg   the error message
	 * @param cause the exception or error that caused this exception to be
	 *              thrown
	 */
	public NestableException(String msg, Throwable cause) {
		super( msg );
		this.cause = cause;
	}

	public Throwable getCause() {
		return cause;
	}

	/**
	 * Returns the detail message string of this throwable. If it was
	 * created with a null message, returns the following:
	 * ( cause==null ? null : cause.toString() ).
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

}
