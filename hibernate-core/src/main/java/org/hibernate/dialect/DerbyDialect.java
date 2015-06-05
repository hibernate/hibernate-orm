/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.lang.reflect.Method;
import java.sql.Types;
import java.util.Locale;

import org.hibernate.MappingException;
import org.hibernate.dialect.function.AnsiTrimFunction;
import org.hibernate.dialect.function.DerbyConcatFunction;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DerbyCaseFragment;

import org.jboss.logging.Logger;

/**
 * Hibernate Dialect for Cloudscape 10 - aka Derby. This implements both an
 * override for the identity column generator as well as for the case statement
 * issue documented at:
 * http://www.jroller.com/comments/kenlars99/Weblog/cloudscape_soon_to_be_derby
 *
 * @author Simon Johnston
 *
 * @deprecated HHH-6073
 */
@Deprecated
public class DerbyDialect extends DB2Dialect {
	@SuppressWarnings("deprecation")
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			DerbyDialect.class.getName()
	);

	private int driverVersionMajor;
	private int driverVersionMinor;
	private final LimitHandler limitHandler;

	/**
	 * Constructs a DerbyDialect
	 */
	@SuppressWarnings("deprecation")
	public DerbyDialect() {
		super();
		if ( this.getClass() == DerbyDialect.class ) {
			LOG.deprecatedDerbyDialect();
		}

		registerFunction( "concat", new DerbyConcatFunction() );
		registerFunction( "trim", new AnsiTrimFunction() );
		registerColumnType( Types.BLOB, "blob" );
		determineDriverVersion();

		if ( driverVersionMajor > 10 || ( driverVersionMajor == 10 && driverVersionMinor >= 7 ) ) {
			registerColumnType( Types.BOOLEAN, "boolean" );
		}

		this.limitHandler = new DerbyLimitHandler();
	}

	private void determineDriverVersion() {
		try {
			// locate the derby sysinfo class and query its version info
			final Class sysinfoClass = ReflectHelper.classForName( "org.apache.derby.tools.sysinfo", this.getClass() );
			final Method majorVersionGetter = sysinfoClass.getMethod( "getMajorVersion", ReflectHelper.NO_PARAM_SIGNATURE );
			final Method minorVersionGetter = sysinfoClass.getMethod( "getMinorVersion", ReflectHelper.NO_PARAM_SIGNATURE );
			driverVersionMajor = (Integer) majorVersionGetter.invoke( null, ReflectHelper.NO_PARAMS );
			driverVersionMinor = (Integer) minorVersionGetter.invoke( null, ReflectHelper.NO_PARAMS );
		}
		catch ( Exception e ) {
			LOG.unableToLoadDerbyDriver( e.getMessage() );
			driverVersionMajor = -1;
			driverVersionMinor = -1;
		}
	}

	private boolean isTenPointFiveReleaseOrNewer() {
		return driverVersionMajor > 10 || ( driverVersionMajor == 10 && driverVersionMinor >= 5 );
	}

	@Override
	public String getCrossJoinSeparator() {
		return ", ";
	}

	@Override
	public CaseFragment createCaseFragment() {
		return new DerbyCaseFragment();
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public boolean supportsSequences() {
		// technically sequence support was added in 10.6.1.0...
		//
		// The problem though is that I am not exactly sure how to differentiate 10.6.1.0 from any other 10.6.x release.
		//
		// http://db.apache.org/derby/docs/10.0/publishedapi/org/apache/derby/tools/sysinfo.html seems incorrect.  It
		// states that derby's versioning scheme is major.minor.maintenance, but obviously 10.6.1.0 has 4 components
		// to it, not 3.
		//
		// Let alone the fact that it states that versions with the matching major.minor are 'feature
		// compatible' which is clearly not the case here (sequence support is a new feature...)
		return driverVersionMajor > 10 || ( driverVersionMajor == 10 && driverVersionMinor >= 6 );
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		if ( supportsSequences() ) {
			return "values next value for " + sequenceName;
		}
		else {
			throw new MappingException( "Derby does not support sequence prior to release 10.6.1.0" );
		}
	}

	@Override
	public boolean supportsLimit() {
		return isTenPointFiveReleaseOrNewer();
	}

	@Override
	public boolean supportsCommentOn() {
		//HHH-4531
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimitOffset() {
		return isTenPointFiveReleaseOrNewer();
	}

	@Override
	public String getForUpdateString() {
		return " for update with rs";
	}

	@Override
	public String getWriteLockString(int timeout) {
		return " for update with rs";
	}

	@Override
	public String getReadLockString(int timeout) {
		return " for read only with rs";
	}


	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
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
	@Override
	public String getLimitString(String query, final int offset, final int limit) {
		final StringBuilder sb = new StringBuilder(query.length() + 50);
		final String normalizedSelect = query.toLowerCase(Locale.ROOT).trim();
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
			sb.append( ' ' );
			sb.append( query.substring( forUpdateIndex ) );
		}
		else if ( hasWithClause( normalizedSelect ) ) {
			sb.append( ' ' ).append( query.substring( getWithIndex( query ) ) );
		}
		return sb.toString();
	}

	@Override
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

	@Override
	public String getQuerySequencesString() {
		return null ;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	private final class DerbyLimitHandler extends AbstractLimitHandler {
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
		@Override
		public String processSql(String sql, RowSelection selection) {
			final StringBuilder sb = new StringBuilder( sql.length() + 50 );
			final String normalizedSelect = sql.toLowerCase(Locale.ROOT).trim();
			final int forUpdateIndex = normalizedSelect.lastIndexOf( "for update" );

			if (hasForUpdateClause( forUpdateIndex )) {
				sb.append( sql.substring( 0, forUpdateIndex - 1 ) );
			}
			else if (hasWithClause( normalizedSelect )) {
				sb.append( sql.substring( 0, getWithIndex( sql ) - 1 ) );
			}
			else {
				sb.append( sql );
			}

			if (LimitHelper.hasFirstRow( selection )) {
				sb.append( " offset " ).append( selection.getFirstRow() ).append( " rows fetch next " );
			}
			else {
				sb.append( " fetch first " );
			}

			sb.append( getMaxOrLimit( selection ) ).append(" rows only" );

			if (hasForUpdateClause( forUpdateIndex )) {
				sb.append( ' ' );
				sb.append( sql.substring( forUpdateIndex ) );
			}
			else if (hasWithClause( normalizedSelect )) {
				sb.append( ' ' ).append( sql.substring( getWithIndex( sql ) ) );
			}
			return sb.toString();
		}

		@Override
		public boolean supportsLimit() {
			return isTenPointFiveReleaseOrNewer();
		}

		@Override
		@SuppressWarnings("deprecation")
		public boolean supportsLimitOffset() {
			return isTenPointFiveReleaseOrNewer();
		}

		@Override
		public boolean supportsVariableLimit() {
			return false;
		}
	}

}
