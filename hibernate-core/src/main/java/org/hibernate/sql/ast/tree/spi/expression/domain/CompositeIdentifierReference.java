/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.LockMode;
import org.hibernate.metamodel.model.domain.spi.EntityIdentifierComposite;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * @author Steve Ebersole
 */
public class CompositeIdentifierReference extends AbstractNavigableContainerReference implements DomainResultProducer {

	public CompositeIdentifierReference(
			EntityValuedNavigableReference entityReference,
			EntityIdentifierComposite referencedNavigable,
			NavigablePath navigablePath) {
		super(
				entityReference,
				referencedNavigable,
				navigablePath,
				// todo (6.0) : should we make this its own ColumnReferenceQualifier?
				//		to limit the available columns
				entityReference.getColumnReferenceQualifier(),
				LockMode.NONE
		);
	}

	@Override
	public EntityIdentifierComposite getNavigable() {
		return ( EntityIdentifierComposite ) super.getNavigable();
	}
}
