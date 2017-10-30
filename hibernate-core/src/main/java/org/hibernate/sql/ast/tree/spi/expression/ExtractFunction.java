/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.metamodel.spi.BasicValuedExpressableType;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class ExtractFunction extends AbstractStandardFunction {
	private final Expression unitToExtract;
	private final Expression extractionSource;
	private final BasicValuedExpressableType returnType;

	public ExtractFunction(Expression unitToExtract, Expression extractionSource, BasicValuedExpressableType returnType) {
		this.unitToExtract = unitToExtract;
		this.extractionSource = extractionSource;
		this.returnType = returnType;
	}

	public Expression getUnitToExtract() {
		return unitToExtract;
	}

	public Expression getExtractionSource() {
		return extractionSource;
	}

	@Override
	public void accept(SqlAstWalker walker) {
		walker.visitExtractFunction( this );
	}

	@Override
	public BasicValuedExpressableType getType() {
		return returnType;
	}

	@Override
	public SqlSelection createSqlSelection(int jdbcPosition) {
		return new SqlSelectionImpl(
				jdbcPosition,
				this,
				getType().getBasicType().getSqlSelectionReader()
		);
	}
}
