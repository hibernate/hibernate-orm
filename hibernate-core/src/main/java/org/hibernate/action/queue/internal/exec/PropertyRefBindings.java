package org.hibernate.action.queue.internal.exec;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import org.hibernate.Hibernate;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.StaleStateException;
import org.hibernate.action.queue.spi.bind.GroupedRowBindPlan;
import org.hibernate.action.queue.spi.bind.JdbcValueBindings;
import org.hibernate.action.queue.spi.bind.MutationBindTemplate;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.plan.FlushOperation;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.engine.jdbc.batch.spi.BatchedResultChecker;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.internal.SingleIdEntityLoaderProvidedQueryImpl;
import org.hibernate.metamodel.mapping.BasicEntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.spi.mutation.MutationType;
import org.hibernate.sql.spi.mutation.jdbc.AbstractJdbcMutation;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;

import static org.hibernate.engine.internal.ManagedTypeHelper.asPersistentAttributeInterceptableOrNull;
import static org.hibernate.proxy.HibernateProxy.extractLazyInitializer;

/// Bindings for a generated mutation whose SQL may depend on whether a referenced
/// unique key is available. The choice is made for each row, including grouped collection rows.
public final class PropertyRefBindings extends JdbcValueBindings {
	private final TableMutation<?> model;
	private final PreparableMutationOperation operation;
	private List<PropertyRefLookup> lookups;
	private List<PropertyRefLookup> previousLookups;
	private PreparableMutationOperation previousOperation;
	private MutationBindTemplate previousTemplate;
	private List<EntityKey> referencedRows;

	PropertyRefBindings(FlushOperation flushOperation, PreparableMutationOperation operation) {
		this( flushOperation, operation, true );
	}

	private PropertyRefBindings(FlushOperation flushOperation, PreparableMutationOperation operation, boolean specialize) {
		super( flushOperation.getMutatingTableDescriptor(), operation );
		this.operation = operation;
		model = specialize && getBindTemplate() != null && flushOperation.getBindingPatch() == null && operation instanceof AbstractJdbcMutation mutation
				? mutation.getMutationModel() : null;
	}

	/// A single prepared row, including the bindings in the order required by its SQL.
	public record BoundRow(PreparableMutationOperation operation, PropertyRefBindings bindings,
			MutationBindTemplate template, List<EntityKey> references) {
		public void beforeStatement(PreparedStatement statement, SharedSessionContractImplementor session) {
			bindings.beforeStatement( template, statement, session );
		}

		public void verifyReferencedRows(SharedSessionContractImplementor session) {
			if ( references != null ) {
				PropertyRefBindings.verifyReferencedRows( references, session );
				if ( operation.getMutationType() == MutationType.INSERT ) {
					throw new StaleStateException( "No row inserted by " + operation.getSqlString() );
				}
			}
		}

		BatchedResultChecker batchResultChecker(OperationResultChecker checker, SharedSessionContractImplementor session) {
			if ( references == null ) {
				return checker == null ? null : checker::checkResult;
			}
			return new BatchedResultChecker() {
				@Override
				public void verifyReferencedRows() {
					PropertyRefBindings.verifyReferencedRows( references, session );
				}

				@Override
				public boolean checkResult(int count, PreparedStatement statement, int position, String sql,
						SessionFactoryImplementor factory) throws SQLException {
					return checker == null || checker.checkResult( count, statement, position, sql, factory );
				}
			};
		}
	}

	public static BoundRow bindRow(FlushOperation operation, SharedSessionContractImplementor session) {
		return bindRow( (PreparableMutationOperation) operation.getJdbcOperation(), operation, -1, session );
	}

	/// Bind the mapped values directly when the driver requires an INSERT ... VALUES statement.
	public static BoundRow bindValuesRow(FlushOperation operation, SharedSessionContractImplementor session) {
		return new PropertyRefBindings( operation, (PreparableMutationOperation) operation.getJdbcOperation(), false )
				.prepareRow( operation, -1, session );
	}

	static BoundRow bindRow(PreparableMutationOperation original, FlushOperation operation, int rowIndex,
			SharedSessionContractImplementor session) {
		return new PropertyRefBindings( operation, original ).prepareRow( operation, rowIndex, session );
	}

	BoundRow prepareRow(FlushOperation operation, int rowIndex, SharedSessionContractImplementor session) {
		final var plan = operation.getBindPlan();
		if ( rowIndex >= 0 && plan instanceof GroupedRowBindPlan grouped ) {
			grouped.bindValues( rowIndex, this, operation, session );
		}
		else {
			plan.bindValues( this, operation, session );
		}
		if ( lookups == null ) {
			return new BoundRow( this.operation, this, getBindTemplate(), null );
		}
		if ( !lookups.equals( previousLookups ) ) {
			specialize( session.getFactory() );
		}
		return new BoundRow( previousOperation, this, previousTemplate, referencedRows );
	}

	public static boolean bindIfUnfetched(
			JdbcValueBindings bindings, ModelPart part, Object value, ParameterUsage usage,
			SharedSessionContractImplementor session, IntPredicate includesColumn) {
		return value != null && bindings instanceof PropertyRefBindings specialized && specialized.model != null
				&& part instanceof EntityAssociationMapping association
				&& association.getSideNature() == ForeignKeyDescriptor.Nature.KEY
				&& !association.isReferenceToPrimaryKey()
				&& specialized.bind( association, value, usage, session, includesColumn );
	}

	private boolean bind(EntityAssociationMapping association, Object value, ParameterUsage usage,
			SharedSessionContractImplementor session, IntPredicate includesColumn) {
		final var fk = association.getForeignKeyDescriptor();
		final var type = association.getAssociatedEntityMappingType();
		final var id = type.getIdentifierMapping();
		final var target = fk.getTargetPart();
		BasicAttributeMapping[] available = null;
		if ( Hibernate.isPropertyInitialized( value, target.getPartName() ) ) {
			// A synthetic component spans real attributes of the entity. Some may be lazy.
			if ( !target.isVirtual() || !(target instanceof EmbeddableValuedModelPart composite) ) {
				return false;
			}
			available = loadedComponents( value, composite );
			if ( available == null ) {
				return false;
			}
		}
		// A scalar subquery already present in the original values would move into the select list.
		if ( model instanceof TableInsertStandard insert && insert.hasValueSubquery()
				&& !session.getJdbcServices().getDialect().getSubquerySupport().supports( SubquerySupport.Feature.SELECT_LIST ) ) {
			return false;
		}
		if ( !fk.getKeyTable().equals( model.getTableName() )
				|| !fk.getTargetTable().equals( id.getContainingTableExpression() )
				|| fk.getTargetTable().equals( model.getTableName() )
				|| !(type.getEntityPersister() instanceof AbstractEntityPersister persister)
				|| persister.getSingleIdLoader() instanceof SingleIdEntityLoaderProvidedQueryImpl<?>
				|| type.hasWhereRestrictions() || type.getSoftDeleteMapping() != null
				|| type.getTemporalMapping() != null || type.getTenantIdMapping() != null
				|| persister.isInherited() || type.hasSubclasses()
				|| type.isAffectedByEnabledFilters( session.getLoadQueryInfluencers(), true ) ) {
			return false;
		}
		final var proxy = extractLazyInitializer( value );
		final var interceptable = asPersistentAttributeInterceptableOrNull(
				proxy != null && !proxy.isUninitialized() ? proxy.getImplementation() : value );
		if ( interceptable != null && interceptable.$$_hibernate_getInterceptor()
				instanceof EnhancementAsProxyLazinessInterceptor interceptor && interceptor.hasWrittenFieldNames() ) {
			// The application may have assigned part of the unique key on this reference.
			return false;
		}
		final var targets = new SelectableMapping[fk.getJdbcTypeCount()];
		fk.visitTargetSelectables( (i, selectable) -> targets[i] = selectable );
		final List<PropertyRefLookup.Column> missing = new ArrayList<>();
		for ( int i = 0; i < targets.length; i++ ) {
			final var source = fk.getSelectable( i );
			final String column = source.getSelectionExpression();
			if ( available != null && available[i] != null
					&& !targets[i].getSelectionExpression().equals( available[i].getSelectionExpression() ) ) {
				return false;
			}
			final var binding = findPlainBinding( column, usage );
			if ( !includesColumn.test( i ) || source.isFormula() || targets[i].isFormula()
					|| !targets[i].getContainingTableExpression().equals( fk.getTargetTable() )
					|| hasBinding( column, usage ) || binding == null ) {
				return false;
			}
			if ( available == null || available[i] == null ) {
				missing.add( new PropertyRefLookup.Column( binding, source, targets[i] ) );
			}
		}
		final Object pk = proxy == null ? id.getIdentifier( value ) : proxy.getInternalIdentifier();
		if ( pk == null ) {
			return false;
		}
		final var components = new Object[id.getJdbcTypeCount()];
		if ( id instanceof BasicEntityIdentifierMapping basic ) {
			components[0] = basic.getJdbcMapping().convertToRelationalValue( pk );
		}
		else {
			id.decompose( pk, (i, component, mapping) -> components[i] = component, session );
		}
		if ( lookups == null ) {
			lookups = new ArrayList<>();
			referencedRows = new ArrayList<>();
		}
		referencedRows.add( session.generateEntityKey( pk, persister ) );
		lookups.add( new PropertyRefLookup( id, usage, missing ) );
		for ( int i = 0; i < targets.length; i++ ) {
			final var source = fk.getSelectable( i );
			final String column = source.getSelectionExpression();
			final var loaded = available == null ? null : available[i];
			if ( loaded == null ) {
				bindValue( components, column, usage );
			}
			else {
				final var entity = proxy == null ? value : proxy.getImplementation();
				bindValue( loaded.disassemble( loaded.getValue( entity ), session ), column, usage );
			}
		}
		return true;
	}

	/// Returns the loaded components of a partially fetched, flat unique key in column order.
	private static BasicAttributeMapping[] loadedComponents(Object value, EmbeddableValuedModelPart target) {
		final var embeddable = target.getEmbeddableTypeDescriptor();
		final var attributes = new BasicAttributeMapping[embeddable.getNumberOfAttributeMappings()];
		boolean missing = false;
		for ( int i = 0; i < attributes.length; i++ ) {
			if ( !(embeddable.getAttributeMapping( i ) instanceof BasicAttributeMapping attribute) ) {
				return null;
			}
			if ( Hibernate.isPropertyInitialized( value, attribute.getAttributeName() ) ) {
				attributes[i] = attribute;
			}
			else {
				missing = true;
			}
		}
		return missing ? attributes : null;
	}

	private ColumnValueBinding findPlainBinding(String column, ParameterUsage usage) {
		final List<ColumnValueBinding> bindings;
		if ( model instanceof TableInsertStandard insert ) {
			bindings = insert.getValueBindings();
		}
		else {
			final var update = (TableUpdateStandard) model;
			bindings = usage == ParameterUsage.SET ? update.getValueBindings() : update.getKeyBindings();
		}
		for ( var binding : bindings ) {
			if ( column.equals( binding.getColumnReference().getColumnExpression() ) ) {
				final var expression = binding.getValueExpression();
				return expression != null && "?".equals( expression.getFragment() )
						&& expression.getParameters().size() == 1 ? binding : null;
			}
		}
		return null;
	}

	@Override
	public void clear() {
		super.clear();
		lookups = null;
		referencedRows = null;
	}

	private void specialize(SessionFactoryImplementor factory) {
		final TableMutation<?> specialized = model instanceof TableInsertStandard insert
				? PropertyRefLookup.insert( insert, lookups, factory )
				: PropertyRefLookup.update( (TableUpdateStandard) model, lookups, factory );
		previousOperation = (PreparableMutationOperation) specialized.createMutationOperation( null, factory );
		previousTemplate = getBindTemplate().project( previousOperation, parameter ->
				parameter instanceof PropertyRefLookup.IdentifierParameter id ? id.component : -1 );
		previousLookups = lookups;
	}

	private static void verifyReferencedRows(List<EntityKey> keys, SharedSessionContractImplementor session) {
		for ( var key : keys ) {
			if ( key.getPersister().getDatabaseSnapshot( key.getIdentifierValue(), session ) == null ) {
				session.getFactory().getEntityNotFoundDelegate().handleEntityNotFound( key.getEntityName(), key.getIdentifierValue() );
				throw new ObjectNotFoundException( key.getIdentifierValue(), key.getEntityName() );
			}
		}
	}
}
