/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ImplicitAttributeFetchBuilder;

/**
 * @author Steve Ebersole
 */
public class ImplicitAttributeFetchMemento implements FetchMemento {
	private final NavigablePath navigablePath;
	private final AttributeMapping attributeMapping;

	public ImplicitAttributeFetchMemento(NavigablePath navigablePath, AttributeMapping attributeMapping) {
		this.navigablePath = navigablePath;
		this.attributeMapping = attributeMapping;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new ImplicitAttributeFetchBuilder( navigablePath, attributeMapping );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

}
