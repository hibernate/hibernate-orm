/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.identity;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.PostInsertIdentityPersister;

/**
 * @author Andrea Boriero
 */
public class Oracle12cGetGeneratedKeysDelegate extends GetGeneratedKeysDelegate {
	private String[] keyColumns;

	public Oracle12cGetGeneratedKeysDelegate(PostInsertIdentityPersister persister, Dialect dialect) {
		super( persister, dialect );
		this.keyColumns = getPersister().getRootTableKeyColumnNames();
		if ( keyColumns.length > 1 ) {
			throw new HibernateException( "Identity generator cannot be used with multi-column keys" );
		}

	}

	@Override
	protected PreparedStatement prepare(String insertSQL, SharedSessionContractImplementor session) throws SQLException {
		return session
				.getJdbcCoordinator()
				.getStatementPreparer()
				.prepareStatement( insertSQL, keyColumns );
	}
}
