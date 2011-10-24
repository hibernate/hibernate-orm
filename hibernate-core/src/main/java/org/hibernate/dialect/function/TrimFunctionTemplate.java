/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.dialect.function;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Defines the basic template support for <tt>TRIM</tt> functions
 *
 * @author Steve Ebersole
 */
public abstract class TrimFunctionTemplate implements SQLFunction {
	public boolean hasArguments() {
		return true;
	}

	public boolean hasParenthesesIfNoArguments() {
		return false;
	}

	public Type getReturnType(Type firstArgument, Mapping mapping) throws QueryException {
		return StandardBasicTypes.STRING;
	}

	public String render(Type firstArgument, List args, SessionFactoryImplementor factory) throws QueryException {
		final Options options = new Options();
		final String trimSource;

		if ( args.size() == 1 ) {
			// we have the form: trim(trimSource)
			trimSource = ( String ) args.get( 0 );
		}
		else if ( "from".equalsIgnoreCase( ( String ) args.get( 0 ) ) ) {
			// we have the form: trim(from trimSource).
			//      This is functionally equivalent to trim(trimSource)
			trimSource = ( String ) args.get( 1 );
		}
		else {
			// otherwise, a trim-specification and/or a trim-character
			// have been specified;  we need to decide which options
			// are present and "do the right thing"
			//
			// potentialTrimCharacterArgIndex = 1 assumes that a
			// trim-specification has been specified.  we handle the
			// exception to that explicitly
			int potentialTrimCharacterArgIndex = 1;
			String firstArg = ( String ) args.get( 0 );
			if ( "leading".equalsIgnoreCase( firstArg ) ) {
				options.setTrimSpecification( Specification.LEADING );
			}
			else if ( "trailing".equalsIgnoreCase( firstArg ) ) {
				options.setTrimSpecification( Specification.TRAILING );
			}
			else if ( "both".equalsIgnoreCase( firstArg ) ) {
				// already the default in Options
			}
			else {
				potentialTrimCharacterArgIndex = 0;
			}

			String potentialTrimCharacter = ( String ) args.get( potentialTrimCharacterArgIndex );
			if ( "from".equalsIgnoreCase( potentialTrimCharacter ) ) {
				trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 1 );
			}
			else if ( potentialTrimCharacterArgIndex + 1 >= args.size() ) {
				trimSource = potentialTrimCharacter;
			}
			else {
				options.setTrimCharacter( potentialTrimCharacter );
				if ( "from".equalsIgnoreCase( ( String ) args.get( potentialTrimCharacterArgIndex + 1 ) ) ) {
					trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 2 );
				}
				else {
					trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 1 );
				}
			}
		}
		return render( options, trimSource, factory );
	}

	protected abstract String render(Options options, String trimSource, SessionFactoryImplementor factory);

	public static class Options {
		public static final String DEFAULT_TRIM_CHARACTER = "' '";

		private String trimCharacter = DEFAULT_TRIM_CHARACTER;
		private Specification trimSpecification = Specification.BOTH;

		public String getTrimCharacter() {
			return trimCharacter;
		}

		public void setTrimCharacter(String trimCharacter) {
			this.trimCharacter = trimCharacter;
		}

		public Specification getTrimSpecification() {
			return trimSpecification;
		}

		public void setTrimSpecification(Specification trimSpecification) {
			this.trimSpecification = trimSpecification;
		}
	}

	public static class Specification {
		public static final Specification LEADING = new Specification( "leading" );
		public static final Specification TRAILING = new Specification( "trailing" );
		public static final Specification BOTH = new Specification( "both" );

		private final String name;

		private Specification(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
