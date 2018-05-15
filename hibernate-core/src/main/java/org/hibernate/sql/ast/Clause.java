/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast;

import java.util.function.Predicate;

import org.hibernate.metamodel.model.domain.spi.StateArrayContributor;

import static org.hibernate.metamodel.model.domain.spi.Writeable.STANDARD_INSERT_INCLUSION_CHECK;
import static org.hibernate.metamodel.model.domain.spi.Writeable.STANDARD_UPDATE_INCLUSION_CHECK;

/**
 * Used to indicate which query clause we are currently processing
 *
 * @author Steve Ebersole
 */
public enum Clause {
	INSERT( STANDARD_INSERT_INCLUSION_CHECK ),
	UPDATE( STANDARD_UPDATE_INCLUSION_CHECK),
	DELETE( STANDARD_UPDATE_INCLUSION_CHECK),
	SELECT( stateArrayContributor -> true ),
	FROM( stateArrayContributor -> true ),
	WHERE( stateArrayContributor -> true ),
	GROUP( stateArrayContributor -> true ),
	HAVING( stateArrayContributor -> true ),
	ORDER( stateArrayContributor -> true ),
	LIMIT( stateArrayContributor -> true ),
	CALL( stateArrayContributor -> true ),
	IRRELEVANT( stateArrayContributor -> true );

	private final Predicate<StateArrayContributor> inclusionChecker;

	Clause(Predicate<StateArrayContributor> inclusionChecker) {
		this.inclusionChecker = inclusionChecker;
	}

	public Predicate<StateArrayContributor> getInclusionChecker() {
		return inclusionChecker;
	}
}
