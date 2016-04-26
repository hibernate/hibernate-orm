/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchstrategyhelper;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.persister.entity.OuterJoinLoadable;
import org.hibernate.persister.entity.UniqueKeyLoadable;
import org.hibernate.persister.walking.internal.FetchStrategyHelper;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.AssociationType;

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
		final FetchStyle fetchStyle = FetchStrategyHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchStrategyHelper.determineFetchTiming(
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
		final FetchStyle fetchStyle = FetchStrategyHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchStrategyHelper.determineFetchTiming(
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
		final FetchStyle fetchStyle = FetchStrategyHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchStrategyHelper.determineFetchTiming(
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
		final FetchStyle fetchStyle = FetchStrategyHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchStrategyHelper.determineFetchTiming(
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
		final FetchStyle fetchStyle = FetchStrategyHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		// batch size is ignored with org.hibernate.FetchMode.JOIN
		assertSame( FetchStyle.JOIN, fetchStyle );
		final FetchTiming fetchTiming = FetchStrategyHelper.determineFetchTiming(
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
		final FetchStyle fetchStyle = FetchStrategyHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		assertSame( FetchStyle.BATCH, fetchStyle );
		final FetchTiming fetchTiming = FetchStrategyHelper.determineFetchTiming(
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
		final FetchStyle fetchStyle = FetchStrategyHelper.determineFetchStyleByMetadata(
				fetchMode,
				associationType,
				sessionFactory()
		);
		// Batch size is ignored with FetchMode.SUBSELECT
		assertSame( FetchStyle.SUBSELECT, fetchStyle );
		final FetchTiming fetchTiming = FetchStrategyHelper.determineFetchTiming(
				fetchStyle,
				associationType,
				sessionFactory()
		);
		assertSame( FetchTiming.DELAYED, fetchTiming );
	}

	private org.hibernate.FetchMode determineFetchMode(Class<?> entityClass, String path) {
		OuterJoinLoadable entityPersister = (OuterJoinLoadable) sessionFactory().getEntityPersister( entityClass.getName() );
		int index = ( (UniqueKeyLoadable) entityPersister ).getPropertyIndex( path );
		return  entityPersister.getFetchMode( index );
	}

	private AssociationType determineAssociationType(Class<?> entityClass, String path) {
		OuterJoinLoadable entityPersister = (OuterJoinLoadable) sessionFactory().getEntityPersister( entityClass.getName() );
		int index = ( (UniqueKeyLoadable) entityPersister ).getPropertyIndex( path );
		return (AssociationType) entityPersister.getSubclassPropertyType( index );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class,
				OtherEntity.class
		};
	}
	@javax.persistence.Entity
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

	@javax.persistence.Entity
	@Table(name="otherentity")
	@BatchSize( size = 5)
	public static class OtherEntity {
		@Id
		@GeneratedValue
		private Long id;
	}

}
