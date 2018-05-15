/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import org.hibernate.sql.results.spi.DomainResultCreationContext;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * @author Steve Ebersole
 */
public class DynamicInstantiationArgument {
	private final DomainResultProducer argumentResultProducer;
	private final String alias;

	@SuppressWarnings("WeakerAccess")
	public DynamicInstantiationArgument(DomainResultProducer argumentResultProducer, String alias) {
		this.argumentResultProducer = argumentResultProducer;
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

	public ArgumentDomainResult buildArgumentDomainResult(
			DomainResultCreationContext creationContext,
			DomainResultCreationState creationState) {
		return new ArgumentDomainResult(
				argumentResultProducer.createDomainResult( alias, creationState, creationContext )
		);
	}
}
