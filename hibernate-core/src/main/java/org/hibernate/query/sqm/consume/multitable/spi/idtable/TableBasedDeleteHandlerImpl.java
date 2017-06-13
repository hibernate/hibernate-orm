/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.query.sqm.consume.multitable.spi.DeleteHandler;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerCreationContext;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerExecutionContext;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;

import org.jboss.logging.Logger;

/**
* @author Steve Ebersole
*/
public class TableBasedDeleteHandlerImpl
		extends AbstractTableBasedHandler
		implements DeleteHandler, SqlAstBuildingContext {
	private static final Logger log = Logger.getLogger( TableBasedDeleteHandlerImpl.class );

	public TableBasedDeleteHandlerImpl(
			SqmDeleteStatement sqmDeleteStatement,
			EntityDescriptor entityDescriptor,
			IdTableSupport idTableSupport,
			IdTable idTableInfo,
			HandlerCreationContext creationContext) {
		super( sqmDeleteStatement, entityDescriptor, idTableSupport, idTableInfo, creationContext );
	}

	@Override
	public SqmDeleteStatement getSqmDeleteOrUpdateStatement() {
		return (SqmDeleteStatement) super.getSqmDeleteOrUpdateStatement();
	}

	@Override
	protected void performMutations(HandlerExecutionContext executionContext) {
		// todo (6.0) : see TableBasedUpdateHandlerImpl#performMutations for general guideline
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getCreationContext().getSessionFactory();
	}

	@Override
	public Callback getCallback() {
		return afterLoadAction -> {};
	}
}
