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
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityValuedNavigable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.results.internal.domain.entity.AbstractEntityMappingNode;
import org.hibernate.sql.results.internal.domain.entity.EntityAssembler;
import org.hibernate.sql.results.internal.domain.entity.EntityRootInitializer;
import org.hibernate.sql.results.spi.AssemblerCreationContext;
import org.hibernate.sql.results.spi.AssemblerCreationState;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.EntityResult;
import org.hibernate.sql.results.spi.Initializer;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Builds an entity-based QueryResult for a NativeQuery
 *
 * @author Steve Ebersole
 */
public class QueryResultBuilderRootEntity
		implements NativeQuery.RootReturn, WrappableQueryResultBuilder, ColumnReferenceQualifier {
	private final String tableAlias;
	private final EntityTypeDescriptor entityDescriptor ;
	private LockMode lockMode = LockMode.READ;

	private List<String> idColumnAliases;
	private String discriminatorColumnAlias;

	private Map<String, AttributeMapping> propertyMappings;

	public QueryResultBuilderRootEntity(String tableAlias, EntityTypeDescriptor entityDescriptor ) {
		this.tableAlias = tableAlias;
		this.entityDescriptor = entityDescriptor;
//		sqlSelectionGroupBuilder = new EntitySqlSelectionGroupOverridableBuilder(  );
	}

	@Override
	public JavaTypeDescriptor getResultType() {
		return entityDescriptor.getJavaTypeDescriptor();
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
	public EntityResult buildReturn(
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		return new EntityResultImpl(
				entityDescriptor,
				tableAlias,
				lockMode,
				creationState,
				creationContext
		);
	}

	@Override
	public String getUniqueIdentifier() {
		return tableAlias;
	}

	@Override
	public TableReference locateTableReference(Table table) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public ColumnReference resolveColumnReference(Column column) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public Expression qualify(QualifiableSqlExpressable sqlSelectable) {
		throw new NotYetImplementedFor6Exception(  );
	}

	public static class EntityResultImpl extends AbstractEntityMappingNode implements EntityResult {
		private final EntityTypeDescriptor entityDescriptor;
		private final String queryResultVariable;

		public EntityResultImpl(
				EntityTypeDescriptor entityDescriptor,
				String queryResultVariable,
				LockMode lockMode,
				DomainResultCreationState creationState,
				DomainResultCreationContext creationContext) {
			super(
					entityDescriptor,
					lockMode,
					new NavigablePath( entityDescriptor.getEntityName() ),
					creationContext,
					creationState
			);

			this.entityDescriptor = entityDescriptor;
			this.queryResultVariable = queryResultVariable;

			afterInitialize( creationState );
		}

		@Override
		public EntityValuedNavigable getEntityValuedNavigable() {
			return entityDescriptor;
		}

		@Override
		public String getResultVariable() {
			return queryResultVariable;
		}

		@Override
		public DomainResultAssembler createResultAssembler(
				Consumer<Initializer> initializerCollector,
				AssemblerCreationState creationOptions,
				AssemblerCreationContext creationContext) {
			final EntityRootInitializer initializer = new EntityRootInitializer(
					this,
					getNavigablePath(),
					LockMode.READ,
					null,
					null,
					null,
					initializerCollector,
					creationContext,
					creationOptions
			);

			return new EntityAssembler( getJavaTypeDescriptor(), initializer );
		}

	}
}
