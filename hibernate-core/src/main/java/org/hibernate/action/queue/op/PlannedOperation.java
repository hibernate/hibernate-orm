/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.op;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.StatementShapeKey;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.cyclebreak.BindingPatch;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/// Represents a single SQL operation to be executed.
///
/// Uses graph-based {@link JdbcOperation} which works with
/// {@link EntityTableDescriptor} instead of legacy {@link org.hibernate.sql.model.TableMapping}.
///
/// @author Steve Ebersole
public class PlannedOperation {
	private final TableDescriptor tableDescriptor;
	private final MutationKind kind;
	private final StatementShapeKey shapeKey;

	// SINGLE operation (one table)
	private final JdbcOperation jdbcOperation;

	// The binder/executor plan for this mutation row
	private final BindPlan bindPlan;

	// Optional directive installed by planner for cycle-breaking (nullable FK null-in-insert)
	private BindingPatch bindingPatch;

	// Captured “intended” FK values when we cycle-break (used for fixup update synthesis)
	private final Map<String, Object> intendedFkValues = new LinkedHashMap<>();

	// Captured “intended” unique constraint values when we cycle-break UPDATE swap cycles
	private final Map<String, Object> intendedUniqueValues = new LinkedHashMap<>();

	// Cached analysis/checker from bind phase (optional)
	private Object cachedValuesAnalysis;
	private Object cachedTableInclusionChecker;

	// metadata
	private final boolean needsIdPrePhase;
	private int ordinal;
	private String origin;

	public PlannedOperation(
			TableDescriptor tableDescriptor,
			MutationKind kind,
			JdbcOperation jdbcOperation,
			BindPlan bindPlan,
			int ordinal,
			String origin) {
		this(tableDescriptor, kind, jdbcOperation, bindPlan, ordinal, origin, false);
	}

	public PlannedOperation(
			TableDescriptor tableDescriptor,
			MutationKind kind,
			JdbcOperation jdbcOperation,
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
			case DELETE -> StatementShapeKey.forDelete(tableDescriptor.name(), this);
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

	public JdbcOperation getJdbcOperation() {
		return jdbcOperation;
	}

	public BindPlan getBindPlan() {
		return bindPlan;
	}

	public BindingPatch getBindingPatch() {
		return bindingPatch;
	}

	public Map<String, Object> getIntendedFkValues() {
		return intendedFkValues;
	}

	public Map<String, Object> getIntendedUniqueValues() {
		return intendedUniqueValues;
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

	public void setCachedInsertValuesAnalysis(Object cachedInsertValuesAnalysis) {
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
}
