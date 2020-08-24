/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmPluralValuedSimplePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;

/**
 * @author Andrea Boriero
 */
public class PluralValuedSimplePathInterpretation<T> extends AbstractSqmPathInterpretation<T> {

	public static SqmPathInterpretation<?> from(SqmPluralValuedSimplePath sqmPath, SqmToSqlAstConverter converter) {
		final TableGroup tableGroup = converter.getFromClauseAccess()
				.findTableGroup( sqmPath.getLhs().getNavigablePath() );

		final PluralAttributeMapping mapping = (PluralAttributeMapping) tableGroup.getModelPart().findSubPart(
				sqmPath.getReferencedPathSource().getPathName(),
				null
		);

		return new PluralValuedSimplePathInterpretation<>(
				null,
				sqmPath,
				mapping,
				tableGroup
		);
	}

	private final Expression sqlExpression;

	private PluralValuedSimplePathInterpretation(
			Expression sqlExpression,
			SqmPluralValuedSimplePath sqmPath,
			PluralAttributeMapping mapping,
			TableGroup tableGroup) {
		super(sqmPath, mapping, tableGroup);
		this.sqlExpression = sqlExpression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
