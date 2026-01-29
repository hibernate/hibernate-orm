/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import jakarta.persistence.sql.ColumnMapping;
import org.hibernate.SessionFactory;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.query.named.ModelPartResultMementoBasic;
import org.hibernate.query.results.spi.ResultBuilderBasicValued;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderBasicModelPart;
import org.hibernate.spi.NavigablePath;

import java.util.function.Consumer;

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
	public Class<?> getResultJavaType() {
		return modelPart.getJavaType().getJavaTypeClass();
	}

	@Override
	public ResultBuilderBasicValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteResultBuilderBasicModelPart( navigablePath, modelPart, columnName );
	}

	@Override
	public <R> ColumnMapping<R> toJpaMapping(SessionFactory sessionFactory) {
		return toJpaMappingElement( sessionFactory );
	}

	@Override
	public <R> ColumnMapping<R> toJpaMappingElement(SessionFactory sessionFactory) {
		//noinspection unchecked
		return ColumnMapping.of( columnName, (Class<R>) getResultJavaType() );
	}
}
