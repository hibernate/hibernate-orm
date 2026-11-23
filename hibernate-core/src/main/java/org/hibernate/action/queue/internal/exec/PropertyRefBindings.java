package org.hibernate.action.queue.internal.exec;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;
import org.hibernate.sql.ast.spi.model.ColumnValueParameter;
import org.hibernate.sql.ast.spi.model.ColumnWriteFragment;
import org.hibernate.sql.ast.spi.model.TableInsertStandard;
import org.hibernate.sql.ast.spi.model.TableMutation;
import org.hibernate.sql.ast.spi.model.TableUpdateStandard;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
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
	private Map<Column, Lookup> lookups;
	private Map<Column, Lookup> previousLookups;
	private PreparableMutationOperation previousOperation;
	private List<EntityKey> referencedRows;

	PropertyRefBindings(FlushOperation flushOperation, PreparableMutationOperation operation) {
		super( flushOperation.getMutatingTableDescriptor(), operation );
		this.operation = operation;
		model = hasBindTemplate() && flushOperation.getBindingPatch() == null && operation instanceof AbstractJdbcMutation mutation
				? mutation.getMutationModel() : null;
	}

	/// A single prepared row, including the bindings in the order required by its SQL.
	public record BoundRow(PreparableMutationOperation operation, PropertyRefBindings bindings, List<EntityKey> references) {
		public void beforeStatement(PreparedStatement statement, SharedSessionContractImplementor session) {
			bindings.bindStatement( operation, statement, session );
		}

		public void verifyReferencedRows(SharedSessionContractImplementor session) {
			if ( references != null ) {
				PropertyRefBindings.verifyReferencedRows( references, session );
				if ( operation.getMutationType() == MutationType.INSERT ) {
					throw new StaleStateException( "No row inserted by " + operation.getSqlString() );
				}
			}
		}
	}

	public static BoundRow bindRow(FlushOperation operation, SharedSessionContractImplementor session) {
		return bindRow( (PreparableMutationOperation) operation.getJdbcOperation(), operation, -1, session );
	}

	static BoundRow bindRow(PreparableMutationOperation original, FlushOperation operation, int rowIndex,
			SharedSessionContractImplementor session) {
		final var row = new PropertyRefBindings( operation, original );
		final var plan = operation.getBindPlan();
		if ( rowIndex >= 0 && plan instanceof GroupedRowBindPlan grouped ) {
			grouped.bindValues( rowIndex, row, operation, session );
		}
		else {
			plan.bindValues( row, operation, session );
		}
		final var effective = row.resolve( session );
		return new BoundRow( effective, row, row.referencedRows );
	}

	private record Column(String name, ParameterUsage usage) {}

	/// A lookup parameter is bound from the indicated component of the target id.
	private static final class IdentifierParameter extends ColumnValueParameter {
		final int component;

		IdentifierParameter(ColumnReference reference, ParameterUsage usage, int component) {
			super( reference, usage );
			this.component = component;
		}
	}

	private record Lookup(EntityIdentifierMapping id, SelectableMapping source, SelectableMapping target, boolean firstColumn) {
		ColumnWriteFragment expression(Column column, boolean existence) {
			final var sql = new StringBuilder();
			sql.append( existence ? "exists (select 1" : "(select " + new ColumnReference( "uk", target ).getExpressionText() )
					.append( " from " ).append( target.getContainingTableExpression() ).append( " uk where " );
			final List<ColumnValueParameter> parameters = new ArrayList<>( id.getJdbcTypeCount() );
			for ( int i = 0; i < id.getJdbcTypeCount(); i++ ) {
				if ( i > 0 ) {
					sql.append( " and " );
				}
				final var selectable = id.getSelectable( i );
				sql.append( new ColumnReference( "uk", selectable ).getExpressionText() ).append( "=?" );
				final var reference = new ColumnReference( (String) null, column.name(), false, null, selectable.getJdbcMapping() );
				parameters.add( new IdentifierParameter( reference, column.usage(), i ) );
			}
			return new ColumnWriteFragment( sql.append( ')' ).toString(), parameters, source );
		}
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
		if ( model instanceof TableInsertStandard
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
		for ( int i = 0; i < targets.length; i++ ) {
			final var source = fk.getSelectable( i );
			final String column = source.getSelectionExpression();
			if ( available != null && available[i] != null
					&& !targets[i].getSelectionExpression().equals( available[i].getSelectionExpression() ) ) {
				return false;
			}
			if ( !includesColumn.test( i ) || source.isFormula() || targets[i].isFormula()
					|| !targets[i].getContainingTableExpression().equals( fk.getTargetTable() )
					|| hasBinding( column, usage ) || !hasPlainParameter( column, usage ) ) {
				return false;
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
			lookups = new LinkedHashMap<>();
			referencedRows = new ArrayList<>();
		}
		referencedRows.add( session.generateEntityKey( pk, persister ) );
		boolean needsCheck = true;
		for ( int i = 0; i < targets.length; i++ ) {
			final var source = fk.getSelectable( i );
			final String column = source.getSelectionExpression();
			final var loaded = available == null ? null : available[i];
			if ( loaded == null ) {
				lookups.put( new Column( column, usage ), new Lookup( id, source, targets[i], needsCheck ) );
				bindValue( components, column, usage );
				needsCheck = false;
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

	@SuppressWarnings("unchecked")
	void bindStatement(PreparableMutationOperation effective, PreparedStatement statement,
			SharedSessionContractImplementor session) {
		if ( effective == operation ) {
			beforeStatement( statement, session );
			return;
		}

		// Repeated occurrences share the source column value. Each lookup parameter
		// selects its own identifier component at its physical JDBC position.
		final var slots = MutationBindTemplate.forOperation( effective ).slots();
		final var parameters = effective.getParameterBinders();
		try {
			for ( int i = 0; i < slots.length; i++ ) {
				final var slot = slots[i];
				if ( hasBinding( slot.columnName(), slot.usage() ) ) {
					final var value = getBoundValue( slot.columnName(), slot.usage() );
					final var jdbcValue = parameters.get( i ) instanceof IdentifierParameter id
							? ( (Object[]) value )[id.component] : resolveValue( value );
					slot.jdbcMapping().getJdbcValueBinder().bind( statement, jdbcValue, slot.jdbcPosition(), session );
				}
			}
		}
		catch (SQLException e) {
			throw session.getJdbcServices().getSqlExceptionHelper().convert( e,
					"Unable to bind property-ref mutation", effective.getSqlString() );
		}
	}

	private boolean hasPlainParameter(String column, ParameterUsage usage) {
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
						&& expression.getParameters().size() == 1;
			}
		}
		return false;
	}

	@Override
	public void clear() {
		super.clear();
		lookups = null;
		referencedRows = null;
	}

	PreparableMutationOperation resolve(SharedSessionContractImplementor session) {
		if ( lookups == null ) {
			return operation;
		}
		if ( lookups.equals( previousLookups ) ) {
			return previousOperation;
		}
		final List<ColumnWriteFragment> checks = new ArrayList<>();
		lookups.forEach( (column, lookup) -> {
			if ( lookup.firstColumn() ) {
				checks.add( lookup.expression( column, true ) );
			}
		} );
		final TableMutation<?> specialized;
		if ( model instanceof TableInsertStandard insert ) {
			final var values = substitute( insert.getValueBindings(), ParameterUsage.SET );
			// A SELECT list cannot contain DEFAULT. Omitting its target column applies the same default.
			values.removeIf( binding -> "default".equalsIgnoreCase( binding.getValueExpression().getFragment() ) );
			specialized = new TableInsertStandard( insert, values, checks );
		}
		else {
			final var update = (TableUpdateStandard) model;
			specialized = new TableUpdateStandard( update, substitute( update.getValueBindings(), ParameterUsage.SET ),
					substitute( update.getKeyBindings(), ParameterUsage.RESTRICT ), checks );
		}
		previousOperation = (PreparableMutationOperation) specialized.createMutationOperation( null, session.getFactory() );
		previousLookups = lookups;
		return previousOperation;
	}

	private static void verifyReferencedRows(List<EntityKey> keys, SharedSessionContractImplementor session) {
		for ( var key : keys ) {
			if ( key.getPersister().getDatabaseSnapshot( key.getIdentifierValue(), session ) == null ) {
				session.getFactory().getEntityNotFoundDelegate().handleEntityNotFound( key.getEntityName(), key.getIdentifierValue() );
				throw new ObjectNotFoundException( key.getIdentifierValue(), key.getEntityName() );
			}
		}
	}

	BatchedResultChecker batchResultChecker(OperationResultChecker checker, SharedSessionContractImplementor session) {
		if ( referencedRows == null ) {
			return checker == null ? null : checker::checkResult;
		}
		// Bindings are reused for the next row. Keep the keys for this batch entry.
		final var keys = referencedRows;
		return new BatchedResultChecker() {
			@Override
			public void verifyReferencedRows() {
				PropertyRefBindings.verifyReferencedRows( keys, session );
			}

			@Override
			public boolean checkResult(int count, PreparedStatement statement, int position, String sql,
					SessionFactoryImplementor factory) throws SQLException {
				return checker == null || checker.checkResult( count, statement, position, sql, factory );
			}
		};
	}

	private List<ColumnValueBinding> substitute(List<ColumnValueBinding> bindings, ParameterUsage usage) {
		final List<ColumnValueBinding> result = new ArrayList<>( bindings.size() );
		for ( var binding : bindings ) {
			final var column = new Column( binding.getColumnReference().getColumnExpression(), usage );
			final var lookup = lookups.get( column );
			result.add( lookup == null ? binding
					: new ColumnValueBinding( binding.getColumnReference(), lookup.expression( column, false ) ) );
		}
		return result;
	}
}
