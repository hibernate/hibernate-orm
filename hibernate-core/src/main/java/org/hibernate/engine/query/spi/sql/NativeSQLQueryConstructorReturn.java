/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi.sql;

import java.util.List;

/**
 * Describes a {@link javax.persistence.ConstructorResult}
 *
 * @author Steve Ebersole
 */
public class NativeSQLQueryConstructorReturn implements NativeSQLQueryReturn {
	private final Class targetClass;
	private final NativeSQLQueryScalarReturn[] columnReturns;

	public NativeSQLQueryConstructorReturn(Class targetClass, List<NativeSQLQueryScalarReturn> columnReturns) {
		this.targetClass = targetClass;
		this.columnReturns = columnReturns.toArray( new NativeSQLQueryScalarReturn[ columnReturns.size() ] );
	}

	public Class getTargetClass() {
		return targetClass;
	}

	public NativeSQLQueryScalarReturn[] getColumnReturns() {
		return columnReturns;
	}

	@Override
	public void traceLog(final TraceLogger logger) {
		logger.writeLine( "Constructor[" );
		logger.writeLine( "    targetClass=" + targetClass + "," );
		logger.writeLine( "    columns=[" );

		TraceLogger nestedLogger = new TraceLogger() {
			@Override
			public void writeLine(String traceLine) {
				logger.writeLine( "    " + traceLine );
			}
		};

		for ( NativeSQLQueryScalarReturn columnReturn : columnReturns ) {
			columnReturn.traceLog( nestedLogger );
		}

		logger.writeLine( "    ]" );
		logger.writeLine( "]" );
	}
}
