/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.persistent;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.hql.internal.ast.HqlSqlWalker;
import org.hibernate.hql.spi.id.IdTableInfo;
import org.hibernate.hql.spi.id.TableBasedUpdateHandlerImpl;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.SelectValues;

import static org.hibernate.hql.spi.id.persistent.Helper.SESSION_ID_COLUMN_NAME;

/**
* @author Steve Ebersole
*/
public class UpdateHandlerImpl extends TableBasedUpdateHandlerImpl {
	private final IdTableInfo idTableInfo;

	public UpdateHandlerImpl(
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
	protected int handlePrependedParametersOnIdSelection(PreparedStatement ps, SharedSessionContractImplementor session, int pos) throws SQLException {
		Helper.INSTANCE.bindSessionIdentifier( ps, session, pos );
		return 1;
	}

	@Override
	protected void handleAddedParametersOnUpdate(PreparedStatement ps, SharedSessionContractImplementor session, int position) throws SQLException {
		Helper.INSTANCE.bindSessionIdentifier( ps, session, position );
	}

	@Override
	protected void releaseFromUse(Queryable persister, SharedSessionContractImplementor session) {
		// clean up our id-table rows
		Helper.INSTANCE.cleanUpRows( idTableInfo.getQualifiedIdTableName(), session );
	}
}
