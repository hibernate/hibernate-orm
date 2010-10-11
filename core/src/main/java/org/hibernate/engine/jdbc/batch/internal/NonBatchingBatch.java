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
package org.hibernate.engine.jdbc.batch.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hibernate.service.jdbc.spi.LogicalConnectionImplementor;
import org.hibernate.jdbc.Expectation;

/**
 * An implementation of {@link org.hibernate.engine.jdbc.batch.spi.Batch} which does not perform batching.  It simply executes each statement as it is
 * encountered.
 *
 * @author Steve Ebersole
 */
public class NonBatchingBatch extends AbstractBatchImpl {
	private static final Logger log = LoggerFactory.getLogger( NonBatchingBatch.class );

	protected NonBatchingBatch(Object key, LogicalConnectionImplementor logicalConnection) {
		super( key, logicalConnection );
	}

	public void addToBatch(Expectation expectation) {
		notifyObserversImplicitExecution();
		for ( Map.Entry<String,PreparedStatement> entry : getStatements().entrySet() ) {
			try {
				final PreparedStatement statement = entry.getValue();
				final int rowCount = statement.executeUpdate();
				expectation.verifyOutcome( rowCount, statement, 0 );
			}
			catch ( SQLException e ) {
				log.error( "sqlexception escaped proxy", e );
				throw getJdbcServices().getSqlExceptionHelper().convert( e, "could not execute batch statement", entry.getKey() );
			}
		}
	}

	protected void doExecuteBatch() {
		// nothing to do
	}
}
