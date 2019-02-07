/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;

/**
 * @author Steve Ebersole
 */
public interface CollectionIndex<J> extends Navigable<J>, TableReferenceContributor {

	String NAVIGABLE_NAME = "{index}";

	SimpleTypeDescriptor<?> getDomainTypeDescriptor();

	enum IndexClassification {
		BASIC,
		EMBEDDABLE,
		ANY,
		ONE_TO_MANY,
		MANY_TO_MANY
	}

	IndexClassification getClassification();

	List<Column> getColumns();

	default boolean canContainSubGraphs() {
		final IndexClassification classification = getClassification();
		return classification == IndexClassification.BASIC || classification == IndexClassification.ANY;
	}

	boolean hasNotNullColumns();

	boolean isSettable();

	int getBaseIndex();

	// todo (6.0) - should this be moved into a super contract?
	default J replace(J originalValue, J targetValue, Object owner, Map copyCache, SessionImplementor session) {
		return getJavaTypeDescriptor().getMutabilityPlan().replace(
				originalValue,
				targetValue,
				owner,
				copyCache,
				session
		);
	}
}
