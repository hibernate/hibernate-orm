/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		CorrelatedPluralJoinInheritanceTest.TagEntity.class,
		CorrelatedPluralJoinInheritanceTest.DataEntity.class,
		CorrelatedPluralJoinInheritanceTest.BatchData.class,
		CorrelatedPluralJoinInheritanceTest.ContinuousData.class,
		CorrelatedPluralJoinInheritanceTest.StoredContinuousData.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16988" )
public class CorrelatedPluralJoinInheritanceTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final TagEntity tagEntityA = new TagEntity( "a" );
			entityManager.persist( tagEntityA );
			final TagEntity tagEntityB = new TagEntity( "b" );
			entityManager.persist( tagEntityB );
			final BatchData batchData = new BatchData();
			batchData.getTags().add( tagEntityA );
			entityManager.persist( batchData );
			final StoredContinuousData storedContinuousData = new StoredContinuousData();
			storedContinuousData.getTags().add( tagEntityA );
			storedContinuousData.getStoredTags().add( tagEntityB );
			entityManager.persist( storedContinuousData );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "from BatchData", BatchData.class ).getResultList().forEach( d -> {
				d.getTags().clear();
				entityManager.remove( d );
			} );
			entityManager.createQuery( "from StoredContinuousData", StoredContinuousData.class )
					.getResultList()
					.forEach( d -> {
						d.getTags().clear();
						d.getStoredTags().clear();
						entityManager.remove( d );
					} );
		} );
	}

	@Test
	public void testOneLevelInheritance(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<BatchData> criteriaQuery = cb.createQuery( BatchData.class );
			final Root<BatchData> root = criteriaQuery.from( BatchData.class );
			criteriaQuery.where( createExistsPredicate( root, criteriaQuery, cb, "tags" ) );
			final List<BatchData> resultList = entityManager.createQuery( criteriaQuery ).getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	@Test
	public void testMultipleLevelInheritance(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<StoredContinuousData> criteriaQuery = cb.createQuery( StoredContinuousData.class );
			final Root<StoredContinuousData> root = criteriaQuery.from( StoredContinuousData.class );
			criteriaQuery.where( createExistsPredicate( root, criteriaQuery, cb, "tags" ) );
			final List<StoredContinuousData> resultList = entityManager.createQuery( criteriaQuery ).getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	@Test
	public void testExplicitTreat(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ContinuousData> criteriaQuery = cb.createQuery( ContinuousData.class );
			final Root<ContinuousData> root = criteriaQuery.from( ContinuousData.class );
			final Subquery<String> subquery = criteriaQuery.subquery( String.class );
			final Root<ContinuousData> root2 = subquery.correlate( root );
			subquery.select( root2.get( "id" ) ).where( cb.treat( root2, StoredContinuousData.class ).join( "storedTags" ).get( "id" ).in( "a", "b" ) );
			criteriaQuery.where( cb.exists( subquery ) );
			final List<ContinuousData> resultList = entityManager.createQuery( criteriaQuery ).getResultList();
			assertThat( resultList ).hasSize( 1 );
		} );
	}

	private <D> Predicate createExistsPredicate(
			Root<D> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder cb, String attributeName) {
		final Subquery<String> subquery = criteriaQuery.subquery( String.class );
		final Root<D> root2 = subquery.correlate( root );
		subquery.select( root2.get( "id" ) ).where( root2.join( attributeName ).get( "id" ).in( "a", "b" ) );
		return cb.exists( subquery );
	}

	@Entity( name = "TagEntity" )
	public static class TagEntity {
		@Id
		private String id;

		public TagEntity() {
		}

		public TagEntity(String id) {
			this.id = id;
		}
	}

	@Entity( name = "DataEntity" )
	public abstract static class DataEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToMany
		@JoinTable( name = "data_tags" )
		private Set<TagEntity> tags = new HashSet<>();

		public Set<TagEntity> getTags() {
			return tags;
		}
	}

	@Entity( name = "BatchData" )
	public static class BatchData extends DataEntity {
	}

	@Entity( name = "ContinuousData" )
	public abstract static class ContinuousData extends DataEntity {
	}

	@Entity( name = "StoredContinuousData" )
	public static class StoredContinuousData extends ContinuousData {
		@ManyToMany
		@JoinTable( name = "storeddata_tags" )
		private Set<TagEntity> storedTags = new HashSet<>();

		public Set<TagEntity> getStoredTags() {
			return storedTags;
		}
	}
}
