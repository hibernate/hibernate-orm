/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.model.domain.internal.AnyDiscriminatorSqmPath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;

public class AnyDiscriminatorPathInterpretation<T> extends AbstractSqmPathInterpretation<T> {
	private final Expression expression;

	public static <T> AnyDiscriminatorPathInterpretation<T> from(
			AnyDiscriminatorSqmPath<?> sqmPath,
			SqmToSqlAstConverter converter) {
		final SqmPath<?> lhs = sqmPath.getLhs();
		final TableGroup tableGroup = converter.getFromClauseAccess().findTableGroup( lhs.getNavigablePath() );
		final ModelPart subPart = tableGroup.getModelPart();

		final DiscriminatedAssociationModelPart mapping;
		if ( subPart instanceof PluralAttributeMapping ) {
			PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) subPart;
			mapping = (DiscriminatedAssociationModelPart) pluralAttributeMapping.getElementDescriptor();
		}
		else {
			mapping = (DiscriminatedAssociationModelPart) subPart;
		}

		final TableReference tableReference = tableGroup.getPrimaryTableReference();
		final Expression expression = converter.getSqlExpressionResolver().resolveSqlExpression(
				tableReference,
				mapping.getDiscriminatorPart()
		);

		return new AnyDiscriminatorPathInterpretation<>(
				sqmPath.getNavigablePath(),
				mapping.getDiscriminatorMapping(),
				tableGroup,
				expression
		);
	}

	public AnyDiscriminatorPathInterpretation(
			NavigablePath navigablePath,
			ModelPart mapping,
			TableGroup tableGroup,
			Expression expression) {
		super( navigablePath, mapping, tableGroup );
		this.expression = expression;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		expression.accept( sqlTreeWalker );
	}
}
