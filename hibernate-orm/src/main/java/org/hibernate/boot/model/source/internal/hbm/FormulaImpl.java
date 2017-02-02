/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
