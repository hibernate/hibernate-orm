/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping.ordering.ast;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.spi.NavigablePath;

/**
 * Represents a domain-path (model part path) used in an order-by fragment
 *
 * @author Steve Ebersole
 */
public interface DomainPath extends OrderingExpression, SequencePart {
	NavigablePath getNavigablePath();

	DomainPath getLhs();

	ModelPart getReferenceModelPart();

	default PluralAttributeMapping getPluralAttribute() {
		return getLhs().getPluralAttribute();
	}

	@Override
	default String toDescriptiveText() {
		return "domain path (" + getNavigablePath().getFullPath() + ")";
	}
}
