/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Jpa(
		annotatedClasses = {
				TreatCorrelatedTest.BaseRoot.class,
				TreatCorrelatedTest.DynamicRoot.class,
				TreatCorrelatedTest.VersionedRoot.class,
				TreatCorrelatedTest.Node.class,
				TreatCorrelatedTest.Version.class
		}
)
@JiraKey("HHH-16680")
public class TreatCorrelatedTest {

	@Test
	public void testCriteriaQuery(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<BaseRoot> query = cb.createQuery( BaseRoot.class );

					Root<BaseRoot> root = query.from( BaseRoot.class );

					Subquery<Long> nodesQuery = query.subquery( Long.class );
					Root<BaseRoot> subQueryRoot = nodesQuery.from( BaseRoot.class );
					nodesQuery.select( subQueryRoot.get( "id" ) );
					Root<VersionedRoot> treatedRoot = cb.treat( subQueryRoot, VersionedRoot.class );
					Join<VersionedRoot, Version> joinTrees = treatedRoot.join( "versions" );
					Join<Version, Node> joinNodes = joinTrees.join( "nodes" );
					nodesQuery.where(
							cb.equal( joinNodes.get( "version" ).get( "owner" ).get( "id" ), root.get( "id" ) )
					);

					query.where(
							cb.and(
									cb.equal( root.get( "type" ), "Dog" ),
									cb.exists( nodesQuery )
							)
					);

					query.select( root );

					List<BaseRoot> result = entityManager.createQuery( query ).getResultList();
				}
		);
	}

	@Entity(name = "BaseRoot")
	public static class BaseRoot {

		@Id
		private Long id;
		private String name;
		private String type;
	}

	@Entity
	@DiscriminatorValue(value = "1")
	public static class DynamicRoot extends BaseRoot {
	}

	@Entity(name = "Node")
	public static class Node {
		@Id
		private Long id;
		@ManyToOne(fetch = FetchType.LAZY)
		private Version version;
	}

	@Entity(name = "Version")
	public static class Version {
		@Id
		private Long id;
		private String name;
		@ManyToOne(fetch = FetchType.LAZY)
		private VersionedRoot owner;
		@OneToMany(fetch = FetchType.LAZY, mappedBy = "version", cascade = CascadeType.PERSIST)
		private List<Node> nodes;
	}

	@Entity
	@DiscriminatorValue(value = "2")
	public static class VersionedRoot extends BaseRoot {

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<Version> versions;
	}
}
