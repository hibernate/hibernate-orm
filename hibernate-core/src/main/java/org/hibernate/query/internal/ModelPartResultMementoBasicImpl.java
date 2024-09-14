/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.ModelPartResultMementoBasic;
import org.hibernate.query.results.ResultBuilderBasicValued;
import org.hibernate.query.results.complete.CompleteResultBuilderBasicModelPart;

/**
 * @author Steve Ebersole
 */
public class ModelPartResultMementoBasicImpl implements ModelPartResultMementoBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart modelPart;
	private final String columnName;

	public ModelPartResultMementoBasicImpl(
			NavigablePath navigablePath,
			BasicValuedModelPart modelPart,
			String columnName) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
		this.columnName = columnName;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ResultBuilderBasicValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteResultBuilderBasicModelPart( navigablePath, modelPart, columnName );
	}
}
