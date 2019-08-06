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
import org.hibernate.query.sqm.spi.SemanticQueryWalker;
import org.hibernate.query.hql.spi.SemanticPathPart;
import org.hibernate.query.sqm.spi.SqmCreationState;

/**
 * @author Steve Ebersole
 */
public class SqmMinIndexPath<T> extends AbstractSqmSpecificPluralPartPath<T> {
	public static final String NAVIGABLE_NAME = "{min-index}";

	private final SqmPathSource<T> indexPathSource;

	public SqmMinIndexPath(SqmPath<?> pluralDomainPath) {
		//noinspection unchecked
		super(
				pluralDomainPath.getNavigablePath().append( NAVIGABLE_NAME ),
				pluralDomainPath,
				(PluralPersistentAttribute<?, ?, T>) pluralDomainPath.getReferencedPathSource()
		);

		if ( getPluralAttribute() instanceof ListPersistentAttribute ) {
			//noinspection unchecked
			this.indexPathSource = ( (ListPersistentAttribute) getPluralAttribute() ).getIndexPathSource();
		}
		else if ( getPluralAttribute() instanceof MapPersistentAttribute ) {
			//noinspection unchecked
			this.indexPathSource = ( (MapPersistentAttribute) getPluralAttribute() ).getKeyPathSource();
		}
		else {
			throw new UnsupportedOperationException( "Plural attribute [" + getPluralAttribute() + "] is not indexed" );
		}
	}

	@Override
	public SemanticPathPart resolvePathPart(
			String name,
			boolean isTerminal,
			SqmCreationState creationState) {
		return indexPathSource.createSqmPath( this, creationState );
	}

	@Override
	public SqmPathSource<T> getReferencedPathSource() {
		return indexPathSource;
	}

	@Override
	public <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitMinIndexPath( this );
	}

}
