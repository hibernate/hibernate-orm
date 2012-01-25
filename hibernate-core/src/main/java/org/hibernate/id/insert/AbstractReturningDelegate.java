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
package org.hibernate.id.insert;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.PostInsertIdentityPersister;
import org.hibernate.pretty.MessageHelper;

/**
 * Abstract InsertGeneratedIdentifierDelegate implementation where the
 * underlying strategy causes the enerated identitifer to be returned as an
 * effect of performing the insert statement.  Thus, there is no need for an
 * additional sql statement to determine the generated identitifer.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractReturningDelegate implements InsertGeneratedIdentifierDelegate {
	private final PostInsertIdentityPersister persister;

	public AbstractReturningDelegate(PostInsertIdentityPersister persister) {
		this.persister = persister;
	}

	public final Serializable performInsert(
			String insertSQL,
			SessionImplementor session,
			Binder binder) {
		try {
			// prepare and execute the insert
			PreparedStatement insert = prepare( insertSQL, session );
			try {
				binder.bindValues( insert );
				return executeAndExtract( insert );
			}
			finally {
				releaseStatement( insert, session );
			}
		}
		catch ( SQLException sqle ) {
			throw session.getFactory().getSQLExceptionHelper().convert(
			        sqle,
			        "could not insert: " + MessageHelper.infoString( persister ),
			        insertSQL
				);
		}
	}

	protected PostInsertIdentityPersister getPersister() {
		return persister;
	}

	protected abstract PreparedStatement prepare(String insertSQL, SessionImplementor session) throws SQLException;

	protected abstract Serializable executeAndExtract(PreparedStatement insert) throws SQLException;

	protected void releaseStatement(PreparedStatement insert, SessionImplementor session) throws SQLException {
		insert.close();
	}
}
