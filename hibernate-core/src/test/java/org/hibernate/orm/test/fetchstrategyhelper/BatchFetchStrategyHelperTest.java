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
import org.hibernate.annotations.BatchSize;
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

import java.util.Set;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(annotatedClasses = {
		BatchFetchStrategyHelperTest.AnEntity.class,
		BatchFetchStrategyHelperTest.OtherEntity.class
})
@SessionFactory
public class BatchFetchStrategyHelperTest {

	@Test
	public void testManyToOneDefaultFetch(SessionFactoryScope factoryScope) {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntityDefault", factoryScope.getSessionFactory() );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "otherEntityDefault", factoryScope.getSessionFactory() );
		Assertions.assertSame( org.hibernate.FetchMode.JOIN, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				factoryScope.getSessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		Assertions.assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testManyToOneJoinFetch(SessionFactoryScope factoryScope) {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntityJoin", factoryScope.getSessionFactory() );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "otherEntityJoin", factoryScope.getSessionFactory() );
		Assertions.assertSame( org.hibernate.FetchMode.JOIN, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				factoryScope.getSessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		Assertions.assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testManyToOneSelectFetch(SessionFactoryScope factoryScope) {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntitySelect", factoryScope.getSessionFactory() );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "otherEntitySelect", factoryScope.getSessionFactory() );
		Assertions.assertSame( org.hibernate.FetchMode.SELECT, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionDefaultFetch(SessionFactoryScope factoryScope) {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsDefault", factoryScope.getSessionFactory() );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "colorsDefault", factoryScope.getSessionFactory() );
		Assertions.assertSame( org.hibernate.FetchMode.SELECT, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionJoinFetch(SessionFactoryScope factoryScope) {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsJoin", factoryScope.getSessionFactory() );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "colorsJoin", factoryScope.getSessionFactory() );
		Assertions.assertSame( org.hibernate.FetchMode.JOIN, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				factoryScope.getSessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		Assertions.assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testCollectionSelectFetch(SessionFactoryScope factoryScope) {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsSelect", factoryScope.getSessionFactory() );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "colorsSelect", factoryScope.getSessionFactory() );
		Assertions.assertSame( org.hibernate.FetchMode.SELECT, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionSubselectFetch(SessionFactoryScope factoryScope) {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsSubselect", factoryScope.getSessionFactory() );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "colorsSubselect", factoryScope.getSessionFactory() );
		Assertions.assertSame( org.hibernate.FetchMode.SELECT, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				factoryScope.getSessionFactory()
		);
		// Batch size is ignored with FetchMode.SUBSELECT
		Assertions.assertSame( FetchStyle.SUBSELECT, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				factoryScope.getSessionFactory()
		);
		Assertions.assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	private org.hibernate.FetchMode determineFetchMode(Class<?> entityClass, String path, SessionFactoryImplementor sessionFactory) {
		AbstractEntityPersister entityPersister = (AbstractEntityPersister)
				sessionFactory.getMappingMetamodel().getEntityDescriptor(entityClass.getName());
		int index = entityPersister.getPropertyIndex( path );
		return  entityPersister.getFetchMode( index );
	}

	private AssociationType determineAssociationType(
			Class<?> entityClass,
			String path,
			SessionFactoryImplementor sessionFactory) {
		AbstractEntityPersister entityPersister = (AbstractEntityPersister)
				sessionFactory.getMappingMetamodel().getEntityDescriptor(entityClass.getName());
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
		@BatchSize( size = 5)
		private Set<String> colorsDefault;

		@ElementCollection
		@Fetch(FetchMode.JOIN)
		@BatchSize( size = 5)
		private Set<String> colorsJoin;

		@ElementCollection
		@Fetch(FetchMode.SELECT)
		@BatchSize( size = 5)
		private Set<String> colorsSelect;

		@ElementCollection
		@Fetch(FetchMode.SUBSELECT)
		@BatchSize( size = 5)
		private Set<String> colorsSubselect;
	}

	@jakarta.persistence.Entity
	@Table(name="otherentity")
	@BatchSize( size = 5)
	public static class OtherEntity {
		@Id
		@GeneratedValue
		private Long id;
	}

}
