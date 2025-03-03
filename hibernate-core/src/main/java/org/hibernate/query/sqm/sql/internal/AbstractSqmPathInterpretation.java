/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractSqmPathInterpretation<T> implements SqmPathInterpretation<T> {

	private final NavigablePath navigablePath;
	private final ModelPart mapping;
	private final TableGroup tableGroup;

	public AbstractSqmPathInterpretation(
			NavigablePath navigablePath,
			ModelPart mapping,
			TableGroup tableGroup) {
		assert navigablePath != null;
		assert mapping != null;
		assert tableGroup != null;

		this.navigablePath = navigablePath;
		this.mapping = mapping;
		this.tableGroup = tableGroup;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ModelPart getExpressionType() {
		return mapping;
	}

	public TableGroup getTableGroup() {
		return tableGroup;
	}

	@Override
	public DomainResult<T> createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState) {
		return mapping.createDomainResult(
				getNavigablePath(),
				tableGroup,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(DomainResultCreationState creationState) {
		mapping.applySqlSelections( getNavigablePath(), tableGroup, creationState );
	}
}
