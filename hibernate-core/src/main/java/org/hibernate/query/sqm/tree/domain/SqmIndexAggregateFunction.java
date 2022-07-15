/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.metamodel.model.domain.ListPersistentAttribute;
import org.hibernate.metamodel.model.domain.MapPersistentAttribute;
import org.hibernate.metamodel.model.domain.PluralPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmCopyContext;

/**
 * @author Steve Ebersole
 */
public class SqmIndexAggregateFunction<T> extends AbstractSqmSpecificPluralPartPath<T> {
	private final SqmPathSource<T> indexPathSource;
	private final String functionName;

	public SqmIndexAggregateFunction(SqmPath<?> pluralDomainPath, String functionName) {
		//noinspection unchecked
		super(
				pluralDomainPath.getNavigablePath().getParent().append( pluralDomainPath.getNavigablePath().getLocalName(), "{" + functionName + "-index}" ),
				pluralDomainPath,
				(PluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource()
		);
		this.functionName = functionName;

		if ( getPluralAttribute() instanceof ListPersistentAttribute ) {
			//noinspection unchecked
			this.indexPathSource = (SqmPathSource<T>) getPluralAttribute().getIndexPathSource();
		}
		else if ( getPluralAttribute() instanceof MapPersistentAttribute ) {
			//noinspection unchecked
			this.indexPathSource = ( (MapPersistentAttribute<?, T, ?>) getPluralAttribute() ).getKeyPathSource();
		}
		else {
			throw new UnsupportedOperationException( "Plural attribute [" + getPluralAttribute() + "] is not indexed" );
		}
	}

	@Override
	public SqmIndexAggregateFunction<T> copy(SqmCopyContext context) {
		final SqmIndexAggregateFunction<T> existing = context.getCopy( this );
		if ( existing != null ) {
			return existing;
		}

		final SqmIndexAggregateFunction<T> path = context.registerCopy(
				this,
				new SqmIndexAggregateFunction<>(
						getLhs().copy( context ),
						functionName
				)
		);
		copyTo( path, context );
		return path;
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
		return indexPathSource;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitIndexAggregateFunction( this );
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append(functionName).append( '(' );
		getLhs().appendHqlString( sb );
		sb.append( ')' );
	}
}
