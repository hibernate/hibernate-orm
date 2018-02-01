/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.domain.SqmPluralAttributeReference;
import org.hibernate.query.sqm.tree.expression.domain.SqmRestrictedCollectionElementReference;

/**
 * @author Steve Ebersole
 */
public interface CollectionElement<J> extends Navigable<J> {

	String NAVIGABLE_NAME = "{element}";

	enum ElementClassification {
		BASIC,
		EMBEDDABLE,
		ANY,
		ONE_TO_MANY,
		MANY_TO_MANY
	}

	ElementClassification getClassification();

	SqmRestrictedCollectionElementReference createIndexedAccessReference(
			SqmPluralAttributeReference pluralAttributeReference,
			SqmExpression selector,
			SqmReferenceCreationContext creationContext);

	// todo (6.0) : another place to consider removing generic access to columns
	//List<Column> getColumns();
}
