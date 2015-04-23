/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.hql.spi.id.persistent;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.IdTableInfo;
import org.hibernate.hql.spi.id.TableBasedDeleteHandlerImpl;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.SelectValues;

import static org.hibernate.hql.spi.id.persistent.Helper.SESSION_ID_COLUMN_NAME;

/**
* @author Steve Ebersole
*/
public class DeleteHandlerImpl extends TableBasedDeleteHandlerImpl {
	private final IdTableInfo idTableInfo;

	public DeleteHandlerImpl(
			SessionFactoryImplementor factory,
			HqlSqlWalker walker,
			IdTableInfo idTableInfo) {
		super( factory, walker, idTableInfo );
		this.idTableInfo = idTableInfo;
	}

	@Override
	protected void addAnyExtraIdSelectValues(SelectValues selectClause) {
		selectClause.addParameter( Types.CHAR, 36 );
	}

	@Override
	protected String generateIdSubselect(Queryable persister, IdTableInfo idTableInfo) {
		return super.generateIdSubselect( persister, idTableInfo ) + " where " + SESSION_ID_COLUMN_NAME + "=?";
	}

	@Override
	protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SessionImplementor session, int pos) throws SQLException {
		Helper.INSTANCE.bindSessionIdentifier( ps, session, pos );
		return 1;
	}

	@Override
	protected void handleAddedParametersOnDelete(PreparedStatement ps, SessionImplementor session) throws SQLException {
		Helper.INSTANCE.bindSessionIdentifier( ps, session, 1 );
	}

	@Override
	protected void releaseFromUse(Queryable persister, SessionImplementor session) {
		// clean up our id-table rows
		Helper.INSTANCE.cleanUpRows( idTableInfo.getQualifiedIdTableName(), session );
	}
}
