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
