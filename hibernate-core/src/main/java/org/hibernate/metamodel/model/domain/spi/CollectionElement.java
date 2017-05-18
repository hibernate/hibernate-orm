/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager.SqlAliasBase;

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

	void applyTableReferenceJoins(
			JoinType joinType,
			SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector);

	// todo (6.0) : another place to consider removing generic access to columns
	List<Column> getColumns();
}
