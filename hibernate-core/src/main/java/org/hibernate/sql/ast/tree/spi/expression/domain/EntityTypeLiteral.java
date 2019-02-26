/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EntityTypeLiteral implements Expression {
	private final EntityTypeDescriptor<?> entityTypeDescriptor;
	private final DiscriminatorDescriptor<?> discriminatorDescriptor;

	public EntityTypeLiteral(EntityTypeDescriptor<?> entityTypeDescriptor) {
		this.entityTypeDescriptor = entityTypeDescriptor;
		this.discriminatorDescriptor = entityTypeDescriptor.getHierarchy().getDiscriminatorDescriptor();
	}

	public EntityTypeDescriptor<?> getEntityTypeDescriptor() {
		return entityTypeDescriptor;
	}

	public DiscriminatorDescriptor<?> getDiscriminatorDescriptor() {
		return discriminatorDescriptor;
	}

	@Override
	public SqlExpressableType getType() {
		return discriminatorDescriptor.getBoundColumn().getExpressableType();
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "Entity-type literal not supported in select-clause" );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitEntityTypeLiteral( this );
	}
}
