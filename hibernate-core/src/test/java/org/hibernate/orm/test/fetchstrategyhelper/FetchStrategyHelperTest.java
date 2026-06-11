/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchstrategyhelper;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.internal.FetchOptionsHelper;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.AssociationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		FetchStrategyHelperTest.AnEntity.class,
		FetchStrategyHelperTest.OtherEntity.class
})
@SessionFactory
public class FetchStrategyHelperTest {
	@Test
	public void testManyToOneDefaultFetch(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntityDefault", sessionFactory );
		final FetchStyle mappingFetchStyle = determineFetchStyle( AnEntity.class, "otherEntityDefault", sessionFactory );
		Assertions.assertSame( FetchStyle.JOIN, mappingFetchStyle );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				mappingFetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testManyToOneJoinFetch(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntityJoin", sessionFactory );
		final FetchStyle mappingFetchStyle = determineFetchStyle( AnEntity.class, "otherEntityJoin", sessionFactory );
		Assertions.assertSame( FetchStyle.JOIN, mappingFetchStyle );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				mappingFetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testManyToOneSelectFetch(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntitySelect", sessionFactory );
		final FetchStyle mappingFetchStyle = determineFetchStyle( AnEntity.class, "otherEntitySelect", sessionFactory );
		Assertions.assertSame( FetchStyle.SELECT, mappingFetchStyle );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				mappingFetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchStyle.SELECT, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionDefaultFetch(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsDefault", sessionFactory );
		final FetchStyle mappingFetchStyle = determineFetchStyle( AnEntity.class, "colorsDefault", sessionFactory );
		Assertions.assertSame( FetchStyle.SELECT, mappingFetchStyle );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				mappingFetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchStyle.SELECT, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionJoinFetch(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsJoin", sessionFactory );
		final FetchStyle mappingFetchStyle = determineFetchStyle( AnEntity.class, "colorsJoin", sessionFactory );
		Assertions.assertSame( FetchStyle.JOIN, mappingFetchStyle );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				mappingFetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testCollectionSelectFetch(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsSelect", sessionFactory );
		final FetchStyle mappingFetchStyle = determineFetchStyle( AnEntity.class, "colorsSelect", sessionFactory );
		Assertions.assertSame( FetchStyle.SELECT, mappingFetchStyle );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				mappingFetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchStyle.SELECT, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionSubselectFetch(SessionFactoryScope factoryScope) {
		final SessionFactoryImplementor sessionFactory = factoryScope.getSessionFactory();

		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsSubselect", sessionFactory );
		final FetchStyle mappingFetchStyle = determineFetchStyle( AnEntity.class, "colorsSubselect", sessionFactory );
		Assertions.assertSame( FetchStyle.SELECT, mappingFetchStyle );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				mappingFetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchStyle.SUBSELECT, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory
		);
		Assertions.assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	private FetchStyle determineFetchStyle(
			@SuppressWarnings("SameParameterValue") Class<?> entityClass,
			String path,
			SessionFactoryImplementor sessionFactory) {
		AbstractEntityPersister entityPersister = (AbstractEntityPersister)
				sessionFactory.getMappingMetamodel().getEntityDescriptor(entityClass.getName());
		//noinspection removal
		int index = entityPersister.getPropertyIndex( path );
		return entityPersister.getFetchStyle( index );
	}

	private AssociationType determineAssociationType(
			@SuppressWarnings("SameParameterValue") Class<?> entityClass,
			String path,
			SessionFactoryImplementor sessionFactory) {
		AbstractEntityPersister entityPersister = (AbstractEntityPersister)
				sessionFactory.getMappingMetamodel().getEntityDescriptor(entityClass.getName());
		//noinspection removal
		int index = entityPersister.getPropertyIndex( path );
		return (AssociationType) entityPersister.getSubclassPropertyType( index );
	}

	@jakarta.persistence.Entity
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

		@ElementCollection
		private Set<String> colorsDefault = new HashSet<String>();

		@ElementCollection
		@Fetch(FetchMode.JOIN)
		private Set<String> colorsJoin = new HashSet<String>();

		@ElementCollection
		@Fetch(FetchMode.SELECT)
		private Set<String> colorsSelect = new HashSet<String>();

		@ElementCollection
		@Fetch(FetchMode.SUBSELECT)
		private Set<String> colorsSubselect = new HashSet<String>();
	}

	@jakarta.persistence.Entity
	@Table(name="otherentity")
	public static class OtherEntity {
		@Id
		@GeneratedValue
		private Long id;
	}

}
