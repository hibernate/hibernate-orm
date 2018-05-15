/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.TrimSpecification;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * @author Steve Ebersole
 */
public class TrimFunction extends AbstractStandardFunction {
	private final TrimSpecification specification;
	private final Expression trimCharacter;
	private final Expression source;

	private final SqlExpressableType type;

	public TrimFunction(
			TrimSpecification specification,
			Expression trimCharacter,
			Expression source,
			SqlAstCreationContext creationContext) {
		this(
				specification,
				trimCharacter,
				source,
				StandardSpiBasicTypes.STRING.getSqlExpressableType( creationContext.getSessionFactory().getTypeConfiguration() ),
				creationContext
		);
	}

	public TrimFunction(
			TrimSpecification specification,
			Expression trimCharacter,
			Expression source,
			SqlExpressableType type,
			SqlAstCreationContext creationContext) {
		this.specification = specification;
		this.trimCharacter = trimCharacter;
		this.source = source;

		this.type = type;
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
	public SqlExpressableType getExpressableType() {
		return type;
	}

	@Override
	public SqlExpressableType getType() {
		return type;
	}
}
