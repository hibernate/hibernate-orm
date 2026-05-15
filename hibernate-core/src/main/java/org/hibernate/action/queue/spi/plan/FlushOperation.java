/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.spi.plan;

import org.hibernate.Incubating;
import org.hibernate.action.queue.spi.MutationKind;
import org.hibernate.action.queue.spi.StatementShapeKey;
import org.hibernate.action.queue.spi.bind.BindPlan;
import org.hibernate.action.queue.internal.cyclebreak.BindingPatch;
import org.hibernate.action.queue.spi.bind.ChainedPostExecutionCallback;
import org.hibernate.action.queue.spi.bind.OperationResultChecker;
import org.hibernate.action.queue.spi.bind.PostExecutionCallback;
import org.hibernate.action.queue.spi.bind.PreExecutionCallback;
import org.hibernate.action.queue.spi.meta.TableDescriptor;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ValuesAnalysis;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/// Represents a single SQL operation to be executed.
///
/// Uses standard [MutationOperation] from the SQL model package.
///
/// @author Steve Ebersole
/// @since 8.0
@Incubating
public class FlushOperation implements OperationResultChecker {
	private final TableDescriptor tableDescriptor;
	private final MutationKind kind;
	private final StatementShapeKey shapeKey;

	// SINGLE operation (one table)
	private final MutationOperation jdbcOperation;

	// The binder/executor plan for this mutation row
	private final BindPlan bindPlan;

	// Optional directive installed by planner for cycle-breaking (nullable FK null-in-insert)
	private BindingPatch bindingPatch;

	// Captured “intended” FK values when we cycle-break (used for fixup update synthesis)
	private Map<String, Object> intendedFkValues;

	// Captured “intended” unique constraint values when we cycle-break UPDATE swap cycles
	private Map<String, Object> intendedUniqueValues;

	// Cached analysis/checker from bind phase (optional)
	private ValuesAnalysis cachedValuesAnalysis;
	private Object cachedTableInclusionChecker;

	// Optional callback to execute immediately after this operation completes
	// Used to ensure post-execution callbacks run before subsequent operations that might
	// remove the entity from persistence context (e.g., DELETE operations)
	private PostExecutionCallback postExecutionCallback;
	private PreExecutionCallback preExecutionCallback;
	private boolean executionSkipped;

	// metadata
	private final boolean needsIdPrePhase;
	private int ordinal;
	private String origin;

	public FlushOperation(
			TableDescriptor tableDescriptor,
			MutationKind kind,
			MutationOperation jdbcOperation,
			BindPlan bindPlan,
			int ordinal,
			String origin) {
		this(tableDescriptor, kind, jdbcOperation, bindPlan, ordinal, origin, false);
	}

	public FlushOperation(
			TableDescriptor tableDescriptor,
			MutationKind kind,
			MutationOperation jdbcOperation,
			BindPlan bindPlan,
			int ordinal,
			String origin,
			boolean needsIdPrePhase) {
		this.tableDescriptor = tableDescriptor;
		this.kind = kind;
		this.jdbcOperation = jdbcOperation;
		this.bindPlan = bindPlan;
		this.ordinal = ordinal;
		this.origin = origin;
		this.needsIdPrePhase = needsIdPrePhase;

		this.shapeKey = switch (kind) {
			case INSERT -> StatementShapeKey.forInsert(tableDescriptor.name(), this);
			case UPDATE -> StatementShapeKey.forUpdate(tableDescriptor.name(), this);
			case UPDATE_ORDER -> StatementShapeKey.forUpdateOrder(tableDescriptor.name(), this);
			case DELETE -> StatementShapeKey.forDelete(tableDescriptor.name(), this);
			case NO_OP -> StatementShapeKey.forNoOp(tableDescriptor.name());
		};
	}

	public TableDescriptor getMutatingTableDescriptor() {
		return tableDescriptor;
	}

	public String getTableExpression() {
		return tableDescriptor.name();
	}

	public MutationKind getKind() {
		return kind;
	}

	public StatementShapeKey getShapeKey() {
		return shapeKey;
	}

	public MutationOperation getJdbcOperation() {
		return jdbcOperation;
	}

	public BindPlan getBindPlan() {
		return bindPlan;
	}

	public OperationResultChecker getOperationResultChecker() {
		return bindPlan.hasOperationResultChecker() ? this : null;
	}

	@Override
	public boolean checkResult(
			int affectedRowCount,
			int batchPosition,
			String sqlString,
			SessionFactoryImplementor sessionFactory) throws SQLException {
		return bindPlan.checkResult( this, affectedRowCount, batchPosition, sqlString, sessionFactory );
	}

	public BindingPatch getBindingPatch() {
		return bindingPatch;
	}

	public Map<String, Object> getIntendedFkValues() {
		return intendedFkValues == null ? Map.of() : intendedFkValues;
	}

	public Map<String, Object> getIntendedUniqueValues() {
		return intendedUniqueValues == null ? Map.of() : intendedUniqueValues;
	}

	public boolean hasIntendedFkValues() {
		return intendedFkValues != null && !intendedFkValues.isEmpty();
	}

	public boolean hasIntendedUniqueValues() {
		return intendedUniqueValues != null && !intendedUniqueValues.isEmpty();
	}

	public boolean hasIntendedFkValue(String column) {
		return intendedFkValues != null && intendedFkValues.containsKey( column );
	}

	public boolean hasIntendedUniqueValue(String column) {
		return intendedUniqueValues != null && intendedUniqueValues.containsKey( column );
	}

	public void addIntendedFkValue(String column, Object value) {
		if ( intendedFkValues == null ) {
			intendedFkValues = new LinkedHashMap<>();
		}
		intendedFkValues.put( column, value );
	}

	public void addIntendedUniqueValue(String column, Object value) {
		if ( intendedUniqueValues == null ) {
			intendedUniqueValues = new LinkedHashMap<>();
		}
		intendedUniqueValues.put( column, value );
	}

	public Object getCachedInsertValuesAnalysis() {
		return cachedValuesAnalysis;
	}

	public Object getCachedTableInclusionChecker() {
		return cachedTableInclusionChecker;
	}

	public int getOrdinal() {
		return ordinal;
	}

	public String getOrigin() {
		return origin;
	}

	public boolean needsIdPrePhase() {
		return needsIdPrePhase;
	}

	public void setBindingPatch(BindingPatch bindingPatch) {
		this.bindingPatch = bindingPatch;
	}

	public void setCachedInsertValuesAnalysis(ValuesAnalysis cachedInsertValuesAnalysis) {
		// todo (GraphBasedActionQueue) : does this ever change after we calculate them in the decomposer?
		this.cachedValuesAnalysis = cachedInsertValuesAnalysis;
	}

	public void setCachedTableInclusionChecker(Object cachedTableInclusionChecker) {
		// todo (GraphBasedActionQueue) : does this ever change after we calculate them in the decomposer?
		this.cachedTableInclusionChecker = cachedTableInclusionChecker;
	}

	public void setOrdinal(int ordinal) {
		this.ordinal = ordinal;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public PostExecutionCallback getPostExecutionCallback() {
		return postExecutionCallback;
	}

	public PreExecutionCallback getPreExecutionCallback() {
		return preExecutionCallback;
	}

	public void setPreExecutionCallback(PreExecutionCallback preExecutionCallback) {
		this.preExecutionCallback = preExecutionCallback;
	}

	public boolean isExecutionSkipped() {
		return executionSkipped;
	}

	public void setExecutionSkipped(boolean executionSkipped) {
		this.executionSkipped = executionSkipped;
	}

	public void setPostExecutionCallback(PostExecutionCallback postExecutionCallback) {
		if ( this.postExecutionCallback == null ) {
			this.postExecutionCallback = postExecutionCallback;
		}
		else {
			this.postExecutionCallback = new ChainedPostExecutionCallback(
					this.postExecutionCallback,
					postExecutionCallback
			);
		}
	}
}
