/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.mutation.spi.idtable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.hibernate.Metamodel;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.relational.spi.PhysicalTable;
import org.hibernate.naming.Identifier;
import org.hibernate.naming.QualifiedTableName;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.SqmMutationStrategyHelper;
import org.hibernate.query.sqm.mutation.spi.DeleteHandler;
import org.hibernate.query.sqm.mutation.spi.HandlerCreationContext;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.UpdateHandler;
import org.hibernate.query.sqm.tree.delete.SqmDeleteStatement;
import org.hibernate.query.sqm.tree.update.SqmUpdateStatement;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractTableBasedMutationStrategy implements SqmMutationStrategy {
	private final Map<EntityTypeDescriptor,IdTable> idTableInfoMap = new HashMap<>();

	protected abstract IdTableSupport getIdTableSupport();

	@Override
	public void prepare(
			Metamodel runtimeMetadata,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {
		runtimeMetadata.visitEntityDescriptors(
				entityDescriptor -> generateIdTableDefinition(
						entityDescriptor,
						sessionFactoryOptions,
						connectionAccess
				)
		);
	}

	protected IdTable getIdTableInfo(EntityTypeDescriptor entityDescriptor) {
		return idTableInfoMap.get( entityDescriptor );
	}

	protected IdTable generateIdTableDefinition(
			EntityTypeDescriptor entityDescriptor,
			SessionFactoryOptions sessionFactoryOptions,
			JdbcConnectionAccess connectionAccess) {
		final IdTable idTable = new IdTable(
				entityDescriptor,
				determineIdTableName( entityDescriptor, sessionFactoryOptions )
		);

		entityDescriptor.getPrimaryTable().getPrimaryKey().getColumns().forEach(
				column -> idTable.addColumn( new IdTableColumn( idTable, column ) )
		);

		if ( getSessionUidSupport() != null && getSessionUidSupport().needsSessionUidColumn() ) {
			getSessionUidSupport().addColumn( idTable );
		}

		idTableInfoMap.put( entityDescriptor, idTable );

		return idTable;
	}

	protected QualifiedTableName determineIdTableName(
			EntityTypeDescriptor entityDescriptor,
			SessionFactoryOptions sessionFactoryOptions) {

		final Identifier entityTableCatalog = entityDescriptor.getPrimaryTable() instanceof PhysicalTable
				? ( (PhysicalTable) entityDescriptor.getPrimaryTable() ).getCatalogName()
				: null;
		final Identifier entityTableSchema = entityDescriptor.getPrimaryTable() instanceof PhysicalTable
				? ( (PhysicalTable) entityDescriptor.getPrimaryTable() ).getSchemaName()
				: null;

		final Identifier catalogToUse;
		final Identifier schemaToUse;

		switch ( getNamespaceHandling() ) {
			case USE_NONE: {
				catalogToUse = null;
				schemaToUse = null;
				break;
			}
			case USE_ENTITY_TABLE_NAMESPACE: {
				catalogToUse = entityTableCatalog;
				schemaToUse = entityTableSchema;
				break;
			}
			case PREFER_SETTINGS: {
				final Identifier configuredCatalog = getConfiguredCatalog();
				final Identifier configuredSchema = getConfiguredSchema();

				// todo (6.0) : if no setting use default (null) or entity namespace?

				catalogToUse = configuredCatalog == null ? entityTableCatalog : configuredCatalog;
				schemaToUse = configuredSchema == null ? entityTableSchema : configuredSchema;
				break;
			}
			default: {
				throw new IllegalArgumentException(
						String.format(
								Locale.ROOT,
								"Unknown NamespaceHandling value [%s] - expecting %s, %s or %s",
								getNamespaceHandling().name(),
								NamespaceHandling.USE_NONE.name(),
								NamespaceHandling.USE_ENTITY_TABLE_NAMESPACE.name(),
								NamespaceHandling.PREFER_SETTINGS.name()
						)
				);
			}
		}

		return new QualifiedTableName(
				catalogToUse,
				schemaToUse,
				getIdTableSupport().determineIdTableName(
						entityDescriptor,
						sessionFactoryOptions
				)
		);

	}

	protected Identifier getConfiguredCatalog() {
		// by default, none
		return null;
	}

	protected Identifier getConfiguredSchema() {
		// by default, none
		return null;
	}

	protected NamespaceHandling getNamespaceHandling() {
		// by default use the entity table's namespace
		return NamespaceHandling.USE_ENTITY_TABLE_NAMESPACE;
	}

	public enum NamespaceHandling {
		USE_NONE,
		USE_ENTITY_TABLE_NAMESPACE,
		PREFER_SETTINGS
	}

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
	public void release(Metamodel runtimeMetadata, JdbcConnectionAccess connectionAccess) {
		idTableInfoMap.clear();
	}

	@Override
	public UpdateHandler buildUpdateHandler(
			SqmUpdateStatement sqmUpdateStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		return SqmMutationStrategyHelper.resolveUpdateHandler(
				sqmUpdateStatement,
				domainParameterXref,
				creationContext,
				this::buildFallbackUpdateHandler
		);
	}

	private UpdateHandler buildFallbackUpdateHandler(
			SqmUpdateStatement sqmUpdateStatement,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		final EntityTypeDescriptor entityDescriptor = sqmUpdateStatement.getTarget()
				.getReferencedNavigable()
				.getEntityDescriptor();

		final TableBasedUpdateHandlerImpl.Builder builder = new TableBasedUpdateHandlerImpl.Builder(
				sqmUpdateStatement,
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

		return builder.build( domainParameterXref, creationContext );
	}

	@Override
	public DeleteHandler buildDeleteHandler(
			SqmDeleteStatement sqmDelete,
			DomainParameterXref domainParameterXref,
			HandlerCreationContext creationContext) {
		return SqmMutationStrategyHelper.resolveDeleteHandler(
				sqmDelete,
				domainParameterXref,
				creationContext,
				() -> {
					final EntityTypeDescriptor entityDescriptor = sqmDelete.getTarget()
							.getReferencedNavigable()
							.getEntityDescriptor();

					final TableBasedDeleteHandlerImpl.Builder builder = new TableBasedDeleteHandlerImpl.Builder(
							sqmDelete,
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

					return builder.build( creationContext, domainParameterXref );
				}
		);
	}

}
