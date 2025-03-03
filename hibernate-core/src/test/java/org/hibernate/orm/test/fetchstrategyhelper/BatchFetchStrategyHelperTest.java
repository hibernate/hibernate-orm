/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchstrategyhelper;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.internal.FetchOptionsHelper;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.type.AssociationType;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.Assert.assertSame;

/**
 * @author Gail Badner
 */
public class BatchFetchStrategyHelperTest extends BaseCoreFunctionalTestCase {

	@Test
	public void testManyToOneDefaultFetch() {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntityDefault" );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "otherEntityDefault" );
		assertSame( org.hibernate.FetchMode.JOIN, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory()
		);
		assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testManyToOneJoinFetch() {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntityJoin" );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "otherEntityJoin" );
		assertSame( org.hibernate.FetchMode.JOIN, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory()
		);
		assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testManyToOneSelectFetch() {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "otherEntitySelect" );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "otherEntitySelect" );
		assertSame( org.hibernate.FetchMode.SELECT, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory()
		);
		assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionDefaultFetch() {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsDefault" );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "colorsDefault" );
		assertSame( org.hibernate.FetchMode.SELECT, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory()
		);
		assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionJoinFetch() {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsJoin" );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "colorsJoin" );
		assertSame( org.hibernate.FetchMode.JOIN, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory()
		);
		assertSame( FetchTiming.IMMEDIATE, fetchTiming );
	}

	@Test
	public void testCollectionSelectFetch() {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsSelect" );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "colorsSelect" );
		assertSame( org.hibernate.FetchMode.SELECT, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory()
		);
		assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	@Test
	public void testCollectionSubselectFetch() {
		final AssociationType associationType = determineAssociationType( AnEntity.class, "colorsSubselect" );
		final org.hibernate.FetchMode fetchMode = determineFetchMode( AnEntity.class, "colorsSubselect" );
		assertSame( org.hibernate.FetchMode.SELECT, fetchMode );
		final FetchStyle fetchStyle = FetchOptionsHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		// Batch size is ignored with FetchMode.SUBSELECT
		assertSame( FetchStyle.SUBSELECT, fetchStyle );
		final FetchTiming fetchTiming = FetchOptionsHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory()
		);
		assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	private org.hibernate.FetchMode determineFetchMode(Class<?> entityClass, String path) {
		AbstractEntityPersister entityPersister = (AbstractEntityPersister)
				sessionFactory().getMappingMetamodel().getEntityDescriptor(entityClass.getName());
		int index = entityPersister.getPropertyIndex( path );
		return  entityPersister.getFetchMode( index );
	}

	private AssociationType determineAssociationType(Class<?> entityClass, String path) {
		AbstractEntityPersister entityPersister = (AbstractEntityPersister)
				sessionFactory().getMappingMetamodel().getEntityDescriptor(entityClass.getName());
		int index = entityPersister.getPropertyIndex( path );
		return (AssociationType) entityPersister.getSubclassPropertyType( index );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class,
				OtherEntity.class
		};
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
		private Set<String> colorsDefault = new HashSet<String>();

		@ElementCollection
		@Fetch(FetchMode.JOIN)
		@BatchSize( size = 5)
		private Set<String> colorsJoin = new HashSet<String>();

		@ElementCollection
		@Fetch(FetchMode.SELECT)
		@BatchSize( size = 5)
		private Set<String> colorsSelect = new HashSet<String>();

		@ElementCollection
		@Fetch(FetchMode.SUBSELECT)
		@BatchSize( size = 5)
		private Set<String> colorsSubselect = new HashSet<String>();
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
