/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchstrategyhelper;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		FetchStrategyDeterminationTests.AnEntity.class,
		FetchStrategyDeterminationTests.OtherEntity.class
})
@SessionFactory
public class FetchStrategyDeterminationTests {

	@Test
	public void testManyToOneDefaultFetch(SessionFactoryScope factoryScope) {
		final EntityPersister entityDescriptor = factoryScope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( AnEntity.class );
		final AttributeMapping attributeMapping = entityDescriptor.findAttributeMapping( "otherEntityDefault" );
		final FetchOptions mappedFetchOptions = attributeMapping.getMappedFetchOptions();
		Assertions.assertEquals( FetchTiming.IMMEDIATE, mappedFetchOptions.getTiming() );
		Assertions.assertEquals( FetchStyle.JOIN, mappedFetchOptions.getStyle() );
	}

	@Test
	public void testManyToOneJoinFetch(SessionFactoryScope factoryScope) {
		final EntityPersister entityDescriptor = factoryScope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( AnEntity.class );
		final AttributeMapping attributeMapping = entityDescriptor.findAttributeMapping( "otherEntityJoin" );
		final FetchOptions mappedFetchOptions = attributeMapping.getMappedFetchOptions();
		Assertions.assertEquals( FetchTiming.IMMEDIATE, mappedFetchOptions.getTiming() );
		Assertions.assertEquals( FetchStyle.JOIN, mappedFetchOptions.getStyle() );
	}

	@Test
	public void testManyToOneSelectFetch(SessionFactoryScope factoryScope) {
		final EntityPersister entityDescriptor = factoryScope.getSessionFactory().getMappingMetamodel().getEntityDescriptor( AnEntity.class );
		final AttributeMapping attributeMapping = entityDescriptor.findAttributeMapping( "otherEntitySelect" );
		final FetchOptions mappedFetchOptions = attributeMapping.getMappedFetchOptions();
		Assertions.assertEquals( FetchTiming.IMMEDIATE, mappedFetchOptions.getTiming() );
		Assertions.assertEquals( FetchStyle.SELECT, mappedFetchOptions.getStyle() );
	}

	@Entity
	@Table(name="entity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private OtherEntity otherEntityDefault;

		@ManyToOne
		@Fetch(FetchMode.JOIN)
		private OtherEntity otherEntityJoin;

		@ManyToOne
		@Fetch(FetchMode.SELECT)
		private OtherEntity otherEntitySelect;

		// @Fetch(FetchMode.SUBSELECT) is not allowed for ToOne associations
	}

	@Entity
	@Table(name="otherentity")
	public static class OtherEntity {
		@Id
		@GeneratedValue
		private Long id;
	}
}
