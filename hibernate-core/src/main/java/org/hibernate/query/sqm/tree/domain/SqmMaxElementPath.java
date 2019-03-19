/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import java.util.function.Supplier;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.NavigableContainer;
import org.hibernate.query.sqm.SemanticException;
import org.hibernate.query.sqm.consume.spi.SemanticQueryWalker;
import org.hibernate.query.sqm.produce.path.spi.SemanticPathPart;
import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class SqmMaxElementPath extends AbstractSqmSpecificPluralPartPath {
	public static final String NAVIGABLE_NAME = "{max-element}";

	public SqmMaxElementPath(SqmPath pluralDomainPath) {
		super(
				pluralDomainPath.getNavigablePath().append( NAVIGABLE_NAME ),
				pluralDomainPath
		);
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			String currentContextKey,
			boolean isTerminal,
			SqmCreationState creationState) {
		if ( getReferencedNavigable() instanceof NavigableContainer<?> ) {
			final Navigable subNavigable = ( (NavigableContainer) getReferencedNavigable() ).findNavigable( name );
			getPluralDomainPath().prepareForSubNavigableReference( subNavigable, isTerminal, creationState );
			return subNavigable.createSqmExpression( this, creationState );
		}

		throw new SemanticException( "Collection element cannot be de-referenced : " + getPluralDomainPath().getNavigablePath() );
	}

	@Override
	public Navigable<?> getReferencedNavigable() {
		return getCollectionDescriptor().getElementDescriptor();
	}

	@Override
	public ExpressableType getExpressableType() {
		return getReferencedNavigable();
	}

	@Override
	public Supplier<? extends ExpressableType> getInferableType() {
		return this::getReferencedNavigable;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitMaxElementPath( this );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return getReferencedNavigable().getJavaTypeDescriptor();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return getReferencedNavigable().getPersistenceType();
	}

	@Override
	public Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
