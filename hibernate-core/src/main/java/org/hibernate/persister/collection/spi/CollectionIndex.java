/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.collection.spi;

import java.util.List;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ForeignKey;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.sql.JoinType;
import org.hibernate.sql.ast.produce.spi.SqlAliasBaseManager;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public interface CollectionIndex<J,T extends Type<J>> extends Navigable<J> {

	String NAVIGABLE_NAME = "{index}";

	enum IndexClassification {
		BASIC,
		EMBEDDABLE,
		ANY,
		ONE_TO_MANY,
		MANY_TO_MANY
	}

	IndexClassification getClassification();

	void applyTableReferenceJoins(
			JoinType joinType,
			SqlAliasBaseManager.SqlAliasBase sqlAliasBase,
			TableReferenceJoinCollector collector);

	List<Column> getColumns();
}
