/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.CollectionValuedNavigable;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeReference extends AbstractNavigableContainerReference implements NavigableContainerReference {

	/**
	 * Ctor for a collection domain result
	 */
	public PluralAttributeReference(
			CollectionValuedNavigable navigable,
			NavigablePath navigablePath,
			ColumnReferenceQualifier columnReferenceQualifier,
			LockMode lockMode) {
		this( null, navigable, navigablePath, columnReferenceQualifier, lockMode );
	}

	/**
	 * Ctor for fetch reference.  Not that either (but not both)
	 * `containerReference` or `valuesQualifier` may be null
	 */
	public PluralAttributeReference(
			NavigableContainerReference containerReference,
			CollectionValuedNavigable navigable,
			NavigablePath navigablePath,
			ColumnReferenceQualifier valuesQualifier,
			LockMode lockMode) {
		super( containerReference, navigable, navigablePath, valuesQualifier, lockMode );
	}

	@Override
	public PluralPersistentAttribute getNavigable() {
		return (PluralPersistentAttribute) super.getNavigable();
	}

	@Override
	public NavigableReference findNavigableReference(String navigableName) {
		return null;
	}

	@Override
	public void addNavigableReference(NavigableReference reference) {

	}

	public ColumnReferenceQualifier getContainerQualifier() {
		if ( getNavigableContainerReference() == null ) {
			return getColumnReferenceQualifier();
		}

		return super.getColumnReferenceQualifier();
	}
}
