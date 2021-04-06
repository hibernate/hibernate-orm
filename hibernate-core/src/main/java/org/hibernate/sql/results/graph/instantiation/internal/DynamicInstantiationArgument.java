/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.instantiation.internal;

import org.hibernate.query.sqm.sql.internal.DomainResultProducer;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationArgument<T> {
	private final ArgumentDomainResult<T> argumentResult;
	private final String alias;

	@SuppressWarnings("WeakerAccess")
	public DynamicInstantiationArgument(
			String alias,
			DomainResultProducer<T> argumentResultProducer,
			DomainResultCreationState creationState) {
		this.argumentResult = new ArgumentDomainResult<>( argumentResultProducer.createDomainResult( alias, creationState ) );
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	public ArgumentDomainResult<T> buildArgumentDomainResult(DomainResultCreationState creationState) {
		return argumentResult;
	}
}
