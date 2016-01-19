/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;

/**
 * @author Steve Ebersole
 */
public class TargetStdoutImpl extends TargetBase {
	final private String delimiter;
	final private Formatter formatter;

	public TargetStdoutImpl() {
		this( null );
	}

	public TargetStdoutImpl(String delimiter) {
		this( delimiter, FormatStyle.NONE.getFormatter() );
	}

	/**
	 * For testing
	 */
	public TargetStdoutImpl(String delimiter, Formatter formatter) {
		this( Collections.<Exception>emptyList(), false, new SqlStatementLogger(), formatter, delimiter );
	}

	public TargetStdoutImpl(List<Exception> exceptionCollector, boolean haltOnError, SqlStatementLogger sqlStatementLogger, Formatter formatter, String delimiter) {
		super( exceptionCollector, haltOnError, sqlStatementLogger, formatter );

		this.formatter = formatter;
		this.delimiter = delimiter;
	}

	@Override
	public boolean acceptsImportScriptActions() {
		return false;
	}

	@Override
	public void prepare() {
	}

	@Override
	public void doAccept(String action) {
		if (formatter != null) {
			action = formatter.format(action);
		}
		if ( delimiter != null ) {
			action += delimiter;
		}
		System.out.println( action );
	}

	@Override
	public void release() {
	}

}
