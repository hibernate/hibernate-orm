/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.plan;

import org.hibernate.action.queue.MutationKind;
import org.hibernate.action.queue.bind.BindPlan;
import org.hibernate.action.queue.cyclebreak.BindingPatch;
import org.hibernate.sql.model.MutationOperation;

import java.util.LinkedHashMap;
import java.util.Map;

/// Represents a single SQL operation to be executed.
///
/// @author Steve Ebersole
public class PlannedOperation {
	private final String tableExpression;
	private final MutationKind kind;

	// SINGLE operation (one table)
	private final MutationOperation operation;

	// The binder/executor plan for this mutation row
	private final BindPlan bindPlan;

	// Optional directive installed by planner for cycle-breaking (nullable FK null-in-insert)
	private BindingPatch bindingPatch;

	// Captured “intended” FK values when we cycle-break (used for fixup update synthesis)
	private final Map<String, Object> intendedFkValues = new LinkedHashMap<>();

	// Cached analysis/checker from bind phase (optional)
	private Object cachedValuesAnalysis;
	private Object cachedTableInclusionChecker;

	// metadata
	private int ordinal;
	private String origin;

	public PlannedOperation(
			String tableExpression,
			MutationKind kind,
			MutationOperation operation,
			BindPlan bindPlan,
			int ordinal,
			String origin) {
		this.tableExpression = tableExpression;
		this.kind = kind;
		this.operation = operation;
		this.bindPlan = bindPlan;
		this.ordinal = ordinal;
		this.origin = origin;
	}

	public String getTableExpression() {
		return tableExpression;
	}

	public MutationKind getKind() {
		return kind;
	}

	public MutationOperation getOperation() {
		return operation;
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
