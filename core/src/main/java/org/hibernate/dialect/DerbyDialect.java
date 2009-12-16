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
package org.hibernate.dialect;

import org.hibernate.Hibernate;
import org.hibernate.QueryException;
import org.hibernate.HibernateException;
import org.hibernate.util.ReflectHelper;
import org.hibernate.engine.Mapping;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.type.Type;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.DerbyConcatFunction;
import org.hibernate.id.TableHiLoGenerator;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DerbyCaseFragment;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hibernate Dialect for Cloudscape 10 - aka Derby. This implements both an 
 * override for the identity column generator as well as for the case statement
 * issue documented at:
 * http://www.jroller.com/comments/kenlars99/Weblog/cloudscape_soon_to_be_derby
 *
 * @author Simon Johnston
 */
public class DerbyDialect extends DB2Dialect {
	private static final Logger log = LoggerFactory.getLogger( DerbyDialect.class );

	private int driverVersionMajor;
	private int driverVersionMinor;

	public DerbyDialect() {
		super();
		registerFunction( "concat", new DerbyConcatFunction() );
		registerFunction( "trim", new SQLFunctionTemplate( Hibernate.STRING, "trim(?1 ?2 ?3 ?4)" ) );
		determineDriverVersion();
	}

	/*package*/ void determineDriverVersion() {
		try {
			// locate the derby sysinfo class and query its version info
			final Class sysinfoClass = ReflectHelper.classForName( "org.apache.derby.tools.sysinfo", this.getClass() );
			final Method majorVersionGetter = sysinfoClass.getMethod( "getMajorVersion", ReflectHelper.NO_PARAM_SIGNATURE );
			final Method minorVersionGetter = sysinfoClass.getMethod( "getMinorVersion", ReflectHelper.NO_PARAM_SIGNATURE );
			driverVersionMajor = ( (Integer) majorVersionGetter.invoke( null, ReflectHelper.NO_PARAMS ) ).intValue();
			driverVersionMinor = ( (Integer) minorVersionGetter.invoke( null, ReflectHelper.NO_PARAMS ) ).intValue();
		}
		catch ( Exception e ) {
			log.warn( "Unable to load/access derby driver class sysinfo to check versions : " + e );
			driverVersionMajor = -1;
			driverVersionMinor = -1;
		}
	}

	/*package*/ boolean isTenPointFiveReleaseOrNewer() {
		return driverVersionMajor > 10 || ( driverVersionMajor == 10 && driverVersionMinor >= 5 );
	}

	public String getCrossJoinSeparator() {
		return ", ";
	}

	/**
	 * This is different in Cloudscape to DB2.
	 */
	public String getIdentityColumnString() {
		return "not null generated always as identity"; //$NON-NLS-1
	}

	/**
	 * Return the case statement modified for Cloudscape.
	 */
	public CaseFragment createCaseFragment() {
		return new DerbyCaseFragment();
	}

	public boolean dropConstraints() {
	      return true;
	}

	public Class getNativeIdentifierGeneratorClass() {
		return TableHiLoGenerator.class;
	}

	public boolean supportsSequences() {
		return false;
	}

	public boolean supportsLimit() {
		return isTenPointFiveReleaseOrNewer();
	}

	public boolean supportsLimitOffset() {
		return isTenPointFiveReleaseOrNewer();
	}

   public String getForUpdateString() {
		return " for update with rs";
   }

	public String getWriteLockString(int timeout) {
		return " for update with rs";
	}

	public String getReadLockString(int timeout) {
		return " for read only with rs";
	}
	

	/**
	 * {@inheritDoc}
	 * <p/>
	 * From Derby 10.5 Docs:
	 * <pre>
	 * Query
	 * [ORDER BY clause]
	 * [result offset clause]
	 * [fetch first clause]
	 * [FOR UPDATE clause]
	 * [WITH {RR|RS|CS|UR}]
	 * </pre>
	 */
	public String getLimitString(String query, final int offset, final int limit) {
		StringBuffer sb = new StringBuffer(query.length() + 50);

		final String normalizedSelect = query.toLowerCase().trim();
		final int forUpdateIndex = normalizedSelect.lastIndexOf( "for update") ;

		if ( hasForUpdateClause( forUpdateIndex ) ) {
			sb.append( query.substring( 0, forUpdateIndex-1 ) );
		}
		else if ( hasWithClause( normalizedSelect ) ) {
			sb.append( query.substring( 0, getWithIndex( query ) - 1 ) );
		}
		else {
			sb.append( query );
		}

		if ( offset == 0 ) {
			sb.append( " fetch first " );
		}
		else {
			sb.append( " offset " ).append( offset ).append( " rows fetch next " );
		}

		sb.append( limit ).append( " rows only" );

		if ( hasForUpdateClause( forUpdateIndex ) ) {
			sb.append(' ');
			sb.append( query.substring( forUpdateIndex ) );
		}
		else if ( hasWithClause( normalizedSelect ) ) {
			sb.append( ' ' ).append( query.substring( getWithIndex( query ) ) );
		}
		return sb.toString();
	}

	public boolean supportsVariableLimit() {
		// we bind the limit and offset values directly into the sql...
		return false;
	}

	private boolean hasForUpdateClause(int forUpdateIndex) {
		return forUpdateIndex >= 0;
	}

	private boolean hasWithClause(String normalizedSelect){
		return normalizedSelect.startsWith( "with ", normalizedSelect.length()-7 );
	}

	private int getWithIndex(String querySelect) {
		int i = querySelect.lastIndexOf( "with " );
		if ( i < 0 ) {
			i = querySelect.lastIndexOf( "WITH " );
		}
		return i;
	}

	public String getQuerySequencesString() {
	   return null ;
	}

	/**
	 * A specialized function template to emulate the ANSI trim function on Derby DB
	 * since it does not support the full trim specification.  However, we cannot even
	 * fully emulate it because there is not standard 'replace' function either. :(
	 */
	public static class DerbyTrimFunctionEmulation implements SQLFunction {
		private static final SQLFunction LEADING_SPACE_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "ltrim( ?1 )");
		private static final SQLFunction TRAILING_SPACE_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "rtrim( ?1 )");
		private static final SQLFunction BOTH_SPACE_TRIM = new SQLFunctionTemplate( Hibernate.STRING, "ltrim( rtrim( ?1 ) )");
		private static final SQLFunction BOTH_SPACE_TRIM_FROM = new SQLFunctionTemplate( Hibernate.STRING, "ltrim( rtrim( ?2 ) )");

		public Type getReturnType(Type columnType, Mapping mapping) throws QueryException {
			return Hibernate.STRING;
		}

		public boolean hasArguments() {
			return true;
		}

		public boolean hasParenthesesIfNoArguments() {
			return false;
		}

		public String render(List args, SessionFactoryImplementor factory) throws QueryException {
			// according to both the ANSI-SQL and EJB3 specs, trim can either take
			// exactly one parameter or a variable number of parameters between 1 and 4.
			// from the SQL spec:
			//
			// <trim function> ::=
			//      TRIM <left paren> <trim operands> <right paren>
			//
			// <trim operands> ::=
			//      [ [ <trim specification> ] [ <trim character> ] FROM ] <trim source>
			//
			// <trim specification> ::=
			//      LEADING
			//      | TRAILING
			//      | BOTH
			//
			// If only <trim specification> is omitted, BOTH is assumed;
			// if <trim character> is omitted, space is assumed
			if ( args.size() == 1 ) {
				// we have the form: trim(trimSource)
				//      so we trim leading and trailing spaces
				return BOTH_SPACE_TRIM.render( args, factory );
			}
			else if ( "from".equalsIgnoreCase( ( String ) args.get( 0 ) ) ) {
				// we have the form: trim(from trimSource).
				//      This is functionally equivalent to trim(trimSource)
				return BOTH_SPACE_TRIM_FROM.render( args, factory );
			}
			else {
				// otherwise, a trim-specification and/or a trim-character
				// have been specified;  we need to decide which options
				// are present and "do the right thing"
				boolean leading = true;         // should leading trim-characters be trimmed?
				boolean trailing = true;        // should trailing trim-characters be trimmed?
				String trimCharacter;    		// the trim-character
				String trimSource;       		// the trim-source

				// potentialTrimCharacterArgIndex = 1 assumes that a
				// trim-specification has been specified.  we handle the
				// exception to that explicitly
				int potentialTrimCharacterArgIndex = 1;
				String firstArg = ( String ) args.get( 0 );
				if ( "leading".equalsIgnoreCase( firstArg ) ) {
					trailing = false;
				}
				else if ( "trailing".equalsIgnoreCase( firstArg ) ) {
					leading = false;
				}
				else if ( "both".equalsIgnoreCase( firstArg ) ) {
				}
				else {
					potentialTrimCharacterArgIndex = 0;
				}

				String potentialTrimCharacter = ( String ) args.get( potentialTrimCharacterArgIndex );
				if ( "from".equalsIgnoreCase( potentialTrimCharacter ) ) {
					trimCharacter = "' '";
					trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 1 );
				}
				else if ( potentialTrimCharacterArgIndex + 1 >= args.size() ) {
					trimCharacter = "' '";
					trimSource = potentialTrimCharacter;
				}
				else {
					trimCharacter = potentialTrimCharacter;
					if ( "from".equalsIgnoreCase( ( String ) args.get( potentialTrimCharacterArgIndex + 1 ) ) ) {
						trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 2 );
					}
					else {
						trimSource = ( String ) args.get( potentialTrimCharacterArgIndex + 1 );
					}
				}

				List argsToUse = new ArrayList();
				argsToUse.add( trimSource );
				argsToUse.add( trimCharacter );

				if ( trimCharacter.equals( "' '" ) ) {
					if ( leading && trailing ) {
						return BOTH_SPACE_TRIM.render( argsToUse, factory );
					}
					else if ( leading ) {
						return LEADING_SPACE_TRIM.render( argsToUse, factory );
					}
					else {
						return TRAILING_SPACE_TRIM.render( argsToUse, factory );
					}
				}
				else {
					throw new HibernateException( "cannot specify trim character when using Derby as Derby does not support the ANSI trim function, not does it support a replace function to properly emmulate it" );
				}
			}
		}
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean supportsLobValueChangePropogation() {
		return false;
	}
}
