/*
 * Hibernate OGM, Domain model persistence for NoSQL datastores
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.tool.schema.spi.Target;

/**
 * Base class for {@link Target} implementations, dealing with logging and exception handling.
 *
 * @author Gunnar Morling
 */
public abstract class TargetBase implements Target {

	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( TargetBase.class );

	private final List<Exception> exceptionCollector;
	private final boolean haltOnError;
	private final SqlStatementLogger sqlStatementLogger;
	private final Formatter formatter;

	protected TargetBase(List<Exception> exceptionCollector, boolean haltOnError, SqlStatementLogger sqlStatementLogger, Formatter formatter) {
		this.exceptionCollector = exceptionCollector;
		this.haltOnError = haltOnError;
		this.sqlStatementLogger = sqlStatementLogger;
		this.formatter = formatter;
	}

	@Override
	public void accept(String action) {
		try {
			sqlStatementLogger.logStatement( action, formatter );
			doAccept( action );
		}
		catch (Exception e) {
			if ( haltOnError ) {
				throw new HibernateException( "Error during DDL export", e );
			}
			exceptionCollector.add( e );
			LOG.unsuccessfulCreate( action );
			LOG.error( e.getMessage() );
		}
	}

	protected abstract void doAccept(String action);

	public List<Exception> getExceptionCollector() {
		return exceptionCollector;
	}

	public boolean isHaltOnError() {
		return haltOnError;
	}
}
