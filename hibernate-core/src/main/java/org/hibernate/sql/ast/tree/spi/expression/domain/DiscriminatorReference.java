/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.metamodel.model.domain.spi.DiscriminatorDescriptor;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.JdbcValueExtractor;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Reference the entity discriminator in the SQL tree.
 *
 * @author Steve Ebersole
 */
public class DiscriminatorReference implements Expression, NavigableReference {
	private final EntityValuedNavigableReference entityReference;
	private final DiscriminatorDescriptor<?> discriminatorDescriptor;
	private final NavigablePath navigablePath;

	public DiscriminatorReference(EntityValuedNavigableReference entityReference) {
		this.entityReference = entityReference;

		this.discriminatorDescriptor = entityReference.getNavigable()
				.getEntityDescriptor()
				.getHierarchy()
				.getDiscriminatorDescriptor();

		navigablePath = entityReference.getNavigablePath().append( discriminatorDescriptor.getNavigableName() );
	}

	@Override
	public EntityValuedNavigableReference getNavigableContainerReference() {
		return entityReference;
	}

	@Override
	public DiscriminatorDescriptor getNavigable() {
		return discriminatorDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ColumnReferenceQualifier getColumnReferenceQualifier() {
		return entityReference.getColumnReferenceQualifier();
	}

	@Override
	public SqlSelection createSqlSelection(
			int jdbcPosition,
			int valuesArrayPosition,
			BasicJavaDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration) {
		final JdbcValueExtractor jdbcValueExtractor = discriminatorDescriptor.getBoundColumn()
				.getExpressableType()
				.getJdbcValueExtractor();
		return new SqlSelectionImpl(
				jdbcPosition,
				valuesArrayPosition,
				this,
				jdbcValueExtractor
		);
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitDiscriminatorReference( this );
	}
}
