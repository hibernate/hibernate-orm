/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationContext;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableContainerReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmNavigableReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.metamodel.spi.NavigableContainerReferenceInfo;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmColumnReference implements SqmExpression, SqmNavigableReference {
	private final SqmFrom sqmFromBase;
	private final String columnName;

	public SqmColumnReference(SqmFrom sqmFromBase, String columnName) {
		this.sqmFromBase = sqmFromBase;
		this.columnName = columnName;
	}

	public SqmFrom getSqmFromBase() {
		return sqmFromBase;
	}

	public String getColumnName() {
		return columnName;
	}

	@Override
	public ExpressableType getExpressableType() {
		return null;
	}

	@Override
	public ExpressableType getInferableType() {
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitExplicitColumnReference( this );
	}

	@Override
	public String asLoggableText() {
		return null;
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}



	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SqmNavigableReference


	@Override
	public SqmNavigableContainerReference getSourceReference() {
		return (SqmNavigableContainerReference) sqmFromBase.getNavigableReference();
	}

	@Override
	public NavigableContainerReferenceInfo getNavigableContainerReferenceInfo() {
		return getSourceReference();
	}

	@Override
	public Navigable getReferencedNavigable() {
		return null;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return null;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class getJavaType() {
		return null;
	}

	@Override
	public String getUniqueIdentifier() {
		return null;
	}

	@Override
	public String getIdentificationVariable() {
		return null;
	}

	@Override
	public EntityTypeDescriptor getIntrinsicSubclassEntityMetadata() {
		return null;
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqmRestrictedCollectionElementReference resolveIndexedAccess(
			SqmExpression selector,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationContext context) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public SqmFrom getExportedFromElement() {
		return sqmFromBase;
	}
}
