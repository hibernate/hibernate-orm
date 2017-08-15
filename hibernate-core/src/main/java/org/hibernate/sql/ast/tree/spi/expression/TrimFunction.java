/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.results.spi.SqlSelectionReader;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.tree.spi.TrimSpecification;
import org.hibernate.sql.results.spi.Selectable;
import org.hibernate.sql.ast.tree.spi.select.Selection;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class TrimFunction implements StandardFunction {
	private final TrimSpecification specification;
	private final Expression trimCharacter;
	private final Expression source;

	public TrimFunction(
			TrimSpecification specification,
			Expression trimCharacter,
			Expression source) {
		this.specification = specification;
		this.trimCharacter = trimCharacter;
		this.source = source;
	}

	public TrimSpecification getSpecification() {
		return specification;
	}

	public Expression getTrimCharacter() {
		return trimCharacter;
	}

	public Expression getSource() {
		return source;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTrimFunction( this );
	}

	@Override
	public SqlSelectionReader getSqlSelectionReader() {
		return null;
	}

	@Override
	public Selection createSelection(
			Expression selectedExpression, String resultVariable) {
		return null;
	}

	@Override
	public ExpressableType getType() {
		return StandardSpiBasicTypes.STRING;
	}

	@Override
	public Selectable getSelectable() {
		return null;
	}
}
