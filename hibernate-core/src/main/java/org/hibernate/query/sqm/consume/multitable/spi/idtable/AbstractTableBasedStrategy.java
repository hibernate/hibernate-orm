/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.consume.multitable.spi.idtable;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.query.sqm.consume.multitable.spi.DeleteHandler;
import org.hibernate.query.sqm.consume.multitable.spi.HandlerCreationContext;
import org.hibernate.query.sqm.consume.multitable.spi.IdTableStrategy;
import org.hibernate.query.sqm.consume.multitable.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.SqmUpdateStatement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableBasedStrategy implements IdTableStrategy {
	private final Map<EntityDescriptor,IdTable> idTableInfoMap = new HashMap<>();

	protected abstract IdTableSupport getIdTableSupport();

	@Override
	public void prepare(
			MetadataImplementor runtimeMetadata,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {
		runtimeMetadata.getTypeConfiguration().getEntityPersisterMap().values().forEach(
				entityDescriptor -> generateIdTableDefinition(
						entityDescriptor,
						runtimeMetadata,
						sessionFactoryOptions,
						connectionAccess
				)
		);
	}

	protected IdTable getIdTableInfo(EntityDescriptor entityDescriptor) {
		return idTableInfoMap.get( entityDescriptor );
	}

	protected IdTable generateIdTableDefinition(
			EntityDescriptor entityDescriptor,
			MetadataImplementor runtimeMetadata,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {
		final IdTable idTable = new IdTable(
				entityDescriptor,
				determineIdTableName( entityDescriptor, sessionFactoryOptions )
		);
		if ( getSessionUidSupport().needsSessionUidColumn() ) {
			getSessionUidSupport().addColumn( idTable );
		}
		idTableInfoMap.put( entityDescriptor, idTable );
		return idTable;
	}

	protected abstract QualifiedTableName determineIdTableName(
			EntityDescriptor entityDescriptor,
			SessionFactoryOptions sessionFactoryOptions);

	protected SessionUidSupport getSessionUidSupport() {
		// by default none
		return null;
	}

	public BeforeUseAction getBeforeUseAction() {
		// by default none
		return null;
	}

	public AfterUseAction getAfterUseAction() {
		// by default none
		return null;
	}

	public IdTableManagementTransactionality getTableManagementTransactionality() {
		// by default none
		return null;
	}

	@Override
	public void release(MetadataImplementor runtimeMetadata, JdbcConnectionAccess connectionAccess) {
		idTableInfoMap.clear();
	}

	@Override
	public UpdateHandler buildUpdateHandler(SqmUpdateStatement sqmUpdateStatement, HandlerCreationContext creationContext) {
		final EntityDescriptor entityDescriptor = sqmUpdateStatement.getEntityFromElement()
				.getNavigableReference()
				.getReferencedNavigable()
				.getEntityDescriptor();

		final TableBasedUpdateHandlerImpl.Builder builder = new TableBasedUpdateHandlerImpl.Builder(
				sqmUpdateStatement,
				entityDescriptor,
				idTableInfoMap.get( entityDescriptor ),
				getIdTableSupport()
		);

		if ( getBeforeUseAction() != null ) {
			builder.setBeforeUseAction( getBeforeUseAction() );
		}

		if ( getAfterUseAction() != null ) {
			builder.setAfterUseAction( getAfterUseAction() );
		}

		if ( getTableManagementTransactionality() != null ) {
			builder.setTransactionality( getTableManagementTransactionality() );
		}

		final SessionUidSupport sessionUidSupport = getSessionUidSupport();
		if ( sessionUidSupport != null ) {
			builder.setSessionUidSupport( sessionUidSupport );
		}

		return builder.build( creationContext );
	}

	@Override
	public DeleteHandler buildDeleteHandler(SqmDeleteStatement sqmDeleteStatement, HandlerCreationContext creationContext) {
		final EntityDescriptor entityDescriptor = sqmDeleteStatement.getEntityFromElement()
				.getNavigableReference()
				.getReferencedNavigable()
				.getEntityDescriptor();

		final TableBasedDeleteHandlerImpl.Builder builder = new TableBasedDeleteHandlerImpl.Builder(
				sqmDeleteStatement,
				entityDescriptor,
				idTableInfoMap.get( entityDescriptor ),
				getIdTableSupport()
		);

		if ( getBeforeUseAction() != null ) {
			builder.setBeforeUseAction( getBeforeUseAction() );
		}

		if ( getAfterUseAction() != null ) {
			builder.setAfterUseAction( getAfterUseAction() );
		}

		if ( getTableManagementTransactionality() != null ) {
			builder.setTransactionality( getTableManagementTransactionality() );
		}

		final SessionUidSupport sessionUidSupport = getSessionUidSupport();
		if ( sessionUidSupport != null ) {
			builder.setSessionUidSupport( sessionUidSupport );
		}

		return builder.build( creationContext );
	}
}
