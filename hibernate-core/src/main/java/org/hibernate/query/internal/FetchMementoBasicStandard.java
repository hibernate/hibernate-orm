/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.internal;

import java.util.function.Consumer;

import jakarta.persistence.sql.FieldMapping;
import jakarta.persistence.sql.MemberMapping;
import jakarta.persistence.sql.ResultSetMapping;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.query.named.FetchMementoBasic;
import org.hibernate.query.results.spi.FetchBuilder;
import org.hibernate.query.results.internal.complete.CompleteFetchBuilderBasicPart;

/**
 * Memento describing a basic-valued fetch.  A basic-value cannot be
 * de-referenced.
 *
 * @author Steve Ebersole
 */
public class FetchMementoBasicStandard implements FetchMementoBasic {
	private final NavigablePath navigablePath;
	private final BasicValuedModelPart fetchedAttribute;
	private final String columnAlias;

	public FetchMementoBasicStandard(
			NavigablePath navigablePath,
			BasicValuedModelPart fetchedAttribute,
			String columnAlias) {
		this.navigablePath = navigablePath;
		this.fetchedAttribute = fetchedAttribute;
		this.columnAlias = columnAlias;
	}

	public static FetchMementoBasicStandard from(
			FieldMapping<?, ?> basicMapping,
			NavigablePath attributePath,
			EntityPersister entityDescriptor,
			BasicAttributeMapping attributeMapping,
			SessionFactoryImplementor factory) {
		return null;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	public BasicValuedModelPart getFetchedAttribute() {
		return fetchedAttribute;
	}

	public String getColumnAlias() {
		return columnAlias;
	}

	@Override
	public FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context) {
		return new CompleteFetchBuilderBasicPart( navigablePath, fetchedAttribute, columnAlias );
	}

	@Override
	public MemberMapping<?> toJpaMemberMapping(Parent container, SessionFactory sessionFactory) {
		return ResultSetMapping.field(
				container.getResultJavaType(),
				fetchedAttribute.getJavaType().getJavaTypeClass(),
				fetchedAttribute.getFetchableName(),
				fetchedAttribute.getSelectableName()
		);
	}
}
