/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import org.hibernate.query.sqm.produce.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.SqmQuery;

/**
 * QuerySpecProcessingState implementation for DML statements
 *
 * @author Steve Ebersole
 */
public class SqmDmlCreationProcessingState extends SqmCreationProcessingStateImpl {

	public SqmDmlCreationProcessingState(
			SqmQuery<?> processingQuery,
			SqmCreationState creationState) {
		super( processingQuery, creationState );
	}
}
