/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.DerivedValueSource;

/**
 * @author Steve Ebersole
 */
class FormulaImpl
		extends AbstractHbmSourceNode
		implements DerivedValueSource {
	private String tableName;
	private final String expression;

	FormulaImpl(MappingDocument mappingDocument, String tableName, String expression) {
		super( mappingDocument );
		this.tableName = tableName;
		this.expression = expression;
	}

	@Override
	public Nature getNature() {
		return Nature.DERIVED;
	}

	@Override
	public String getExpression() {
		return expression;
	}

	@Override
	public String getContainingTableName() {
		return tableName;
	}
}
