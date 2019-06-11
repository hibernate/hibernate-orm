/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.spi;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.tree.select.SqmSelectQuery;
import org.hibernate.query.sqm.tree.select.SqmSelection;

/**
 * SqmCreationProcessingState specialization for processing a SQM query-spec
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SqmQuerySpecCreationProcessingState extends SqmCreationProcessingState {
	void registerSelection(SqmSelection selection);
	SqmSelection findSelectionByAlias(String alias);
	SqmSelection findSelectionByPosition(int position);

	@Override
	SqmSelectQuery<?> getProcessingQuery();
}
