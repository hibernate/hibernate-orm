/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sql.spi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.QueryException;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeBasic;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEmbedded;
import org.hibernate.metamodel.model.domain.internal.SingularPersistentAttributeEntity;
import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierCompositeNonAggregated;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierSimple;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.RowIdDescriptor;
import org.hibernate.metamodel.model.domain.spi.TenantDiscrimination;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.metamodel.spi.EntityValuedExpressableType;
import org.hibernate.sql.exec.results.internal.AbstractFetchParent;
import org.hibernate.sql.exec.results.internal.EntityReturnInitializerImpl;
import org.hibernate.sql.exec.results.internal.QueryResultAssemblerEntity;
import org.hibernate.sql.exec.results.spi.EntitySqlSelectionMappingBuildingVisitationStrategy;
import org.hibernate.sql.exec.results.spi.InitializerCollector;
import org.hibernate.sql.exec.results.spi.InitializerEntity;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;
import org.hibernate.sql.exec.results.spi.QueryResultEntity;

/**
 * Builds an entity-based QueryResult for a NativeQuery
 *
 * @author Steve Ebersole
 */
public class QueryResultBuilderRootEntity
		implements NativeQuery.RootReturn, WrappableQueryResultBuilder {
	private final String tableAlias;
	private final String entityName;
	private LockMode lockMode = LockMode.READ;

	private List<String> idColumnAliases;
	private String discriminatorColumnAlias;

	private Map<String, AttributeMapping> propertyMappings;

	public QueryResultBuilderRootEntity(String tableAlias, String entityName) {
		this.tableAlias = tableAlias;
		this.entityName = entityName;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NativeQuery.RootReturn

	public NativeQuery.RootReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public NativeQuery.RootReturn addIdColumnAliases(String... aliases) {
		if ( aliases != null ) {
			if ( aliases.length == 1 ) {
				idColumnAliases.add( aliases[0] );
			}
			else {
				idColumnAliases.addAll( Arrays.asList( aliases ) );
			}
		}

		return this;
	}

	public NativeQuery.RootReturn setDiscriminatorAlias(String alias) {
		this.discriminatorColumnAlias = alias;
		return this;
	}

	public NativeQuery.RootReturn addProperty(String propertyName, String columnAlias) {
		addProperty( propertyName ).addColumnAlias( columnAlias );
		return this;
	}

	public NativeQuery.ReturnProperty addProperty(final String propertyName) {
		if ( propertyMappings == null ) {
			propertyMappings = new HashMap<>();
		}

		return new NativeQuery.ReturnProperty() {
			public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
				final AttributeMapping registration = propertyMappings.computeIfAbsent(
						propertyName,
						AttributeMapping::new
				);
				registration.addColumnAlias( columnAlias );
				return this;
			}
		};
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NativeQueryReturnBuilder

	@Override
	public QueryResultEntity buildReturn(NodeResolutionContext resolutionContext) {
		return new QueryResultEntityImpl(
				resolutionContext.getSessionFactory().getTypeConfiguration().resolveEntityDescriptor( entityName ),
				// todo (6.0) - is `tableAlias` the correct thing here?
				//		this is supposed to be the "query result variable" associated with this QueryResult
				//		- is that the intention of `tableAlias`?
				tableAlias,
				idColumnAliases,
				discriminatorColumnAlias,
				propertyMappings,
				resolutionContext
		);
	}

	public static class QueryResultEntityImpl extends AbstractFetchParent implements QueryResultEntity {
		private final EntityDescriptor entityDescriptor;
		private final String queryResultVariable;

		private final EntityReturnInitializerImpl initializer;
		private final QueryResultAssemblerEntity assembler;

		public QueryResultEntityImpl(
				EntityDescriptor entityDescriptor,
				String queryResultVariable,
				List<String> explicitIdColumnAliases,
				String explicitDiscriminatorColumnAlias,
				Map<String, AttributeMapping> explicitAttributeMapping,
				NodeResolutionContext resolutionContext) {
			super( null, new NavigablePath( entityDescriptor.getEntityName() ) );

			this.entityDescriptor = entityDescriptor;
			this.queryResultVariable = queryResultVariable;

			final OverridableEntitySqlSelectionMappingBuilder strategy = new OverridableEntitySqlSelectionMappingBuilder(
					entityDescriptor,
					// row-id
					null,
					explicitIdColumnAliases,
					explicitDiscriminatorColumnAlias,
					// tenant-discriminator
					null,
					explicitAttributeMapping,
					resolutionContext
			);


			getEntityDescriptor().visitNavigables( strategy );

			this.initializer = new EntityReturnInitializerImpl(
					entityDescriptor,
					strategy.generateMappings(),
					false
			);

			this.assembler = new QueryResultAssemblerEntity(
					entityDescriptor.getJavaTypeDescriptor(),
					initializer
			);
		}

		@Override
		public EntityDescriptor getEntityDescriptor() {
			return entityDescriptor;
		}

		@Override
		public String getResultVariable() {
			return queryResultVariable;
		}

		@Override
		public EntityValuedExpressableType getType() {
			return entityDescriptor;
		}

		@Override
		public QueryResultAssembler getResultAssembler() {
			return assembler;
		}

		@Override
		public void registerInitializers(InitializerCollector collector) {
			collector.addInitializer( initializer );
			registerFetchInitializers( initializer, collector );
		}

		@Override
		public InitializerEntity getInitializer() {
			return initializer;
		}

	}

	// todo (6.0 - need some form of SqlSelection, etc distinctions here to support duplicated columns - including fetches (potential duplicated unqualified column name which need to  be unique).

	private static class OverridableEntitySqlSelectionMappingBuilder extends EntitySqlSelectionMappingBuildingVisitationStrategy {
		private final String explicitRowIdColumnAlias;
		private final List<String> explicitIdColumnAliases;
		private final String explicitDiscriminatorColumnAlias;
		private final String explicitTenantDiscriminatorColumnAlias;
		private final Map<String, AttributeMapping> explicitAttributeMapping;

		public OverridableEntitySqlSelectionMappingBuilder(
				EntityDescriptor entityDescriptor,
				String explicitRowIdColumnAlias,
				List<String> explicitIdColumnAliases,
				String explicitDiscriminatorColumnAlias,
				String explicitTenantDiscriminatorColumnAlias,
				Map<String, AttributeMapping> explicitAttributeMapping,
				NodeResolutionContext nodeResolutionContext) {
			super( entityDescriptor, nodeResolutionContext );
			this.explicitRowIdColumnAlias = explicitRowIdColumnAlias;
			this.explicitIdColumnAliases = explicitIdColumnAliases;
			this.explicitDiscriminatorColumnAlias = explicitDiscriminatorColumnAlias;
			this.explicitTenantDiscriminatorColumnAlias = explicitTenantDiscriminatorColumnAlias;
			this.explicitAttributeMapping = explicitAttributeMapping;
		}

		@Override
		public void prepareForVisitation() {
		}

		@Override
		public void visitRowIdDescriptor(RowIdDescriptor rowIdDescriptor) {
			throw new NotYetImplementedException(  );
		}

		@Override
		public void visitTenantTenantDiscrimination(TenantDiscrimination tenantDiscrimination) {
			throw new NotYetImplementedException(  );
		}

		@Override
		public void visitSimpleIdentifier(EntityIdentifierSimple identifier) {
			if ( explicitIdColumnAliases == null || explicitIdColumnAliases.isEmpty() ) {
				super.visitSimpleIdentifier( identifier );
			}
			else {
				// user explicitly mapped the id column - use that explicit info

				// make sure they gave us just one column...
				if ( explicitIdColumnAliases.size() > 1 ) {
					throw new QueryException(
							String.format(
									Locale.ROOT,
									"NativeQuery result-set-mapping included explicit id mapping for entity [%s] - " +
											"but explicit mapping defined %s columns while entity has single id column",
									getEntityDescriptor().getEntityName(),
									explicitIdColumnAliases.size()
							)
					);
				}

				setIdSqlSelectionGroup( identifier.resolveSqlSelectionGroup( getCreationContext() ) );
			}
		}

		@Override
		public void visitAggregateCompositeIdentifier(EntityIdentifierCompositeAggregated identifier) {
			setIdSqlSelectionGroup( identifier.resolveSqlSelectionGroup( getCreationContext() ) );
		}

		@Override
		public void visitNonAggregateCompositeIdentifier(EntityIdentifierCompositeNonAggregated identifier) {
			setIdSqlSelectionGroup( identifier.resolveSqlSelectionGroup( getCreationContext() ) );
		}

		@Override
		public void visitDiscriminator(DiscriminatorDescriptor discriminator) {
			setDiscriminatorSqlSelection( discriminator.resolveSqlSelectionGroup( getCreationContext() ) );
		}

		@Override
		public void visitSingularAttributeBasic(SingularPersistentAttributeBasic attribute) {
			throw new NotYetImplementedException(  );
		}

		@Override
		public void visitSingularAttributeEmbedded(SingularPersistentAttributeEmbedded attribute) {
			throw new NotYetImplementedException(  );
		}

		@Override
		public void visitSingularAttributeEntity(SingularPersistentAttributeEntity attribute) {
			throw new NotYetImplementedException(  );
		}

		@Override
		public void visitPluralAttribute(PluralPersistentAttribute attribute) {
			throw new NotYetImplementedException(  );
		}
	}
}
