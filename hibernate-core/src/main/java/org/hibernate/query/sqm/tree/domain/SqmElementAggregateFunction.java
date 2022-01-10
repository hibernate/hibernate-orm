/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.SqmPathSource;

/**
 * @author Steve Ebersole
 */
public class SqmElementAggregateFunction<T> extends AbstractSqmSpecificPluralPartPath<T> {
	public static final String NAVIGABLE_NAME = "{max-element}";

	private final String functionName;

	public SqmElementAggregateFunction(SqmPath<?> pluralDomainPath, String functionName) {
		//noinspection unchecked
		super(
				pluralDomainPath.getNavigablePath().getParent().append( pluralDomainPath.getNavigablePath().getLocalName(), NAVIGABLE_NAME ),
				pluralDomainPath,
				(PluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource()
		);
		this.functionName = functionName;
	}

	public String getFunctionName() {
		return functionName;
	}

	@Override
	public SqmPath<?> resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		final SqmPath<?> sqmPath = get( name );
		creationState.getProcessingStateStack().getCurrent().getPathRegistry().register( sqmPath );
		return sqmPath;
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return getPluralAttribute().getElementPathSource();
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitElementAggregateFunction( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append(functionName).append( "(" );
		getLhs().appendHqlString( sb );
		sb.append( ')' );
	}
}
