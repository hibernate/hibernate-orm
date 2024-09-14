/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.named.ResultMemento;
import org.hibernate.query.results.ResultSetMapping;

/**
 * Standard {@link NamedResultSetMappingMemento} implementation
 *
 * @author Steve Ebersole
 */
public class NamedResultSetMappingMementoImpl implements NamedResultSetMappingMemento {
	private final String name;
	private final List<ResultMemento> resultMementos;

	public NamedResultSetMappingMementoImpl(
			String name,
			List<ResultMemento> resultMementos) {
		this.name = name;
		this.resultMementos = resultMementos;
	}

	@Override
	public String getName() {
		return name;
	}

	public List<ResultMemento> getResultMementos() {
		return Collections.unmodifiableList( resultMementos );
	}

	@Override
	public void resolve(
			ResultSetMapping resultSetMapping,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		resultMementos.forEach(
				memento -> resultSetMapping.addResultBuilder( memento.resolve( querySpaceConsumer, context ) )
		);
	}
}
