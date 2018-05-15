/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;

/**
 * @author Steve Ebersole
 */
public class ExtractFunction extends AbstractStandardFunction {
	private final Expression unitToExtract;
	private final Expression extractionSource;
	private final SqlExpressableType returnType;

	public ExtractFunction(Expression unitToExtract, Expression extractionSource, SqlExpressableType returnType) {
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
	public SqlExpressableType getExpressableType() {
		return returnType;
	}

	@Override
	public SqlExpressableType getType() {
		return returnType;
	}
}
