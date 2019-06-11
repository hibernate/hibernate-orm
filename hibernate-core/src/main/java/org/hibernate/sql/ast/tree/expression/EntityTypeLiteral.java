/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.expression;

import org.hibernate.persister.SqlExpressableType;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class EntityTypeLiteral implements Expression {
	private final EntityPersister entityTypeDescriptor;
	private final Type discriminatorType;

	public EntityTypeLiteral(EntityPersister entityTypeDescriptor) {
		this.entityTypeDescriptor = entityTypeDescriptor;
		this.discriminatorType = ( (Queryable) entityTypeDescriptor ).getDiscriminatorType();
	}

	public EntityPersister getEntityTypeDescriptor() {
		return entityTypeDescriptor;
	}

	@Override
	public SqlExpressableType getType() {
		return (SqlExpressableType) discriminatorType;
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		throw new UnsupportedOperationException( "Entity-type literal not supported in select-clause" );
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitEntityTypeLiteral( this );
	}
}
