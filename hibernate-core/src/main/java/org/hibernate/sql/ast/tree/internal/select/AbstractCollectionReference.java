/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.internal.select;

import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.sql.ast.tree.spi.select.CollectionReference;
import org.hibernate.sql.ast.tree.spi.expression.domain.NavigableReference;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionReference implements CollectionReference {
	private final NavigableReference navigableReference;
	private final String resultVariable;

	private final PersistentCollectionDescriptor collectionMetadata;

	protected AbstractCollectionReference(NavigableReference navigableReference, String resultVariable) {
		this.navigableReference = navigableReference;
		this.resultVariable = resultVariable;

		this.collectionMetadata = ( (PluralPersistentAttribute) navigableReference.getNavigable() ).getPersistentCollectionMetadata();
	}

	public NavigableReference getNavigableReference() {
		return navigableReference;
	}

	public String getResultVariable() {
		return resultVariable;
	}

	@Override
	public PersistentCollectionDescriptor getCollectionMetadata() {
		return collectionMetadata;
	}
}
