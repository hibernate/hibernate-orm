/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.spi.SqlExpressionResolver;
import org.hibernate.sql.results.internal.domain.basic.BasicResultImpl;
import org.hibernate.sql.results.spi.DomainResult;
import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.internal.ColumnBasedMapper;

/**
 * @author Steve Ebersole
 */
public class CollectionIdentifier implements DomainResultProducer, BasicValuedNavigable<Object> {
	public static final String NAVIGABLE_NAME = "{collection-id}";

	private final PersistentCollectionDescriptor collectionDescriptor;

	private final Column column;
	private final BasicValueMapper valueMapper;

	private final IdentifierGenerator generator;

	private final NavigableRole navigableRole;


	public <O,C,E> CollectionIdentifier(
			PersistentCollectionDescriptor<O,C,E> collectionDescriptor,
			Column column,
			IdentifierGenerator generator) {
		this.collectionDescriptor = collectionDescriptor;

		this.column = column;
		this.valueMapper = new ColumnBasedMapper( column );

		this.generator = generator;

		this.navigableRole = collectionDescriptor.getNavigableRole().append( NAVIGABLE_NAME );
	}


	public IdentifierGenerator getGenerator() {
		return generator;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// DomainResultProducer

	@Override
	public DomainResult createDomainResult(
			String resultVariable,
			DomainResultCreationState creationState,
			DomainResultCreationContext creationContext) {
		final SqlExpressionResolver sqlExpressionResolver = creationState.getSqlExpressionResolver();

		return new BasicResultImpl(
				resultVariable,
				sqlExpressionResolver.resolveSqlSelection(
						sqlExpressionResolver.resolveSqlExpression(
								creationState.getColumnReferenceQualifierStack().getCurrent(),
								column
						),
						column.getJavaTypeDescriptor(),
						creationContext.getSessionFactory().getTypeConfiguration()
				),
				column.getExpressableType()
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Navigable

	@Override
	public NavigableContainer<?> getContainer() {
		return collectionDescriptor;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitCollectionIdentifier( this );
	}

	@Override
	public String asLoggableText() {
		return navigableRole.getFullPath();
	}

	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<Object> getJavaTypeDescriptor() {
		return (BasicJavaDescriptor<Object>) column.getJavaTypeDescriptor();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Column getBoundColumn() {
		return column;
	}

	@Override
	public BasicValueMapper<Object> getValueMapper() {
		return valueMapper;
	}
}
