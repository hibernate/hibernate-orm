/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeReference extends AbstractNavigableReference {
	public PluralAttributeReference(
			NavigableContainerReference containerReference,
			PluralPersistentAttribute referencedAttribute,
			NavigablePath navigablePath) {
		// todo (6.0) : need a ColumnReferenceQualifer covering the owner table, the "collection table" and any element/index table
		super( containerReference, referencedAttribute, navigablePath, containerReference.getSqlExpressionQualifier() );
	}

	@Override
	public PluralPersistentAttribute getNavigable() {
		return (PluralPersistentAttribute) super.getNavigable();
	}

	@Override
	public ColumnReferenceQualifier getSqlExpressionQualifier() {
		// todo (6.0) : this really needs a composite ColumnReferenceQualifier
		//		combining collection-table, element table and index table
		//
		throw new NotYetImplementedFor6Exception();
	}
}
