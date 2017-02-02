/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast;
import java.io.PrintStream;
import java.io.PrintWriter;

import antlr.SemanticException;

/**
 * Thrown when a call to the underlying Hibernate engine fails, indicating
 * some form of semantic exception (e.g. a class name was not found in the
 * current mappings, etc.).
 */
public class DetailedSemanticException extends SemanticException {
	private Throwable cause;
	private boolean showCauseMessage = true;

	public DetailedSemanticException(String message) {
		super( message );
	}

	public DetailedSemanticException(String s, Throwable e) {
		super( s );
		cause = e;
	}

	/**
	 * Converts everything to a string.
	 *
	 * @return a string.
	 */
	public String toString() {
		if ( cause == null || ( !showCauseMessage ) ) {
			return super.toString();
		}
		else {
			return super.toString() + "\n[cause=" + cause.toString() + "]";
		}
	}

	/**
	 * Prints a stack trace.
	 */
	public void printStackTrace() {
		super.printStackTrace();
		if ( cause != null ) {
			cause.printStackTrace();
		}
	}

	/**
	 * Prints a stack trace to the specified print stream.
	 *
	 * @param s the print stream.
	 */
	public void printStackTrace(PrintStream s) {
		super.printStackTrace( s );
		if ( cause != null ) {
			s.println( "Cause:" );
			cause.printStackTrace( s );
		}
	}

	/**
	 * Prints this throwable and its backtrace to the specified print writer.
	 *
	 * @param w the print writer.s
	 */
	public void printStackTrace(PrintWriter w) {
		super.printStackTrace( w );
		if ( cause != null ) {
			w.println( "Cause:" );
			cause.printStackTrace( w );
		}
	}

}
