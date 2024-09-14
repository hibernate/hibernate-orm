/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMemento;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.complete.CompleteFetchBuilderEntityValuedModelPart;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;

/**
 * @author Christian Beikov
 */
public class FetchMementoEntityStandard implements FetchMemento {
	private final NavigablePath navigablePath;
	private final EntityValuedFetchable attributeMapping;
	private final List<String> columnNames;

	public FetchMementoEntityStandard(
			NavigablePath navigablePath,
			EntityValuedFetchable attributeMapping,
			List<String> columnNames) {
		this.navigablePath = navigablePath;
		this.attributeMapping = attributeMapping;
		this.columnNames = columnNames;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteFetchBuilderEntityValuedModelPart( navigablePath, attributeMapping, columnNames );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

}
