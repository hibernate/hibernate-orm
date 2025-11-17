/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.embedded;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				EmbeddedCircularFetchTests.LeafEntity.class,
				EmbeddedCircularFetchTests.RootEntity.class
		}
)
@SessionFactory
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "jpa"),
		@Setting(name = AvailableSettings.GENERATE_STATISTICS, value = "true")
})
public class EmbeddedCircularFetchTests {
	// todo (6.0 : this (along with the `org.hibernate.orm.test.sql.exec.onetoone.bidirectional` package)
	//  	probably makes better sense in a dedicated `org.hibernate.orm.test.fetch.circular` package:
	//		- `org.hibernate.orm.test.fetch.circular.embedded`
	//		- `org.hibernate.orm.test.fetch.circular.onetoone`
	//		- `org.hibernate.orm.test.fetch.circular.manytoone`

	@Test
	@JiraKey(value = "HHH-9642")
	public void testCircularFetchAcrossComponent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RootEntity root = new RootEntity();
					root.id = 1;
					session.persist( root );

					LeafEntity leafEntity1 = new LeafEntity();
					leafEntity1.id = 10;

					LeafEntity leafEntity2 = new LeafEntity();
					leafEntity2.id = 11;

					session.persist( leafEntity1 );
					session.persist( leafEntity2 );

					leafEntity2.rootEntity = root;
					leafEntity1.rootEntity = root;
					root.intermediateComponent = new IntermediateComponent();

					root.intermediateComponent.leaves = new HashSet<>(  );
					root.intermediateComponent.leaves.add( leafEntity1 );
					root.intermediateComponent.leaves.add( leafEntity2 );
				}
		);

		scope.inTransaction(
				session -> {
					session.getSessionFactory().getStatistics().clear();
					final RootEntity result = session.createQuery(
							"from RootEntity r " +
									"join fetch r.intermediateComponent.leaves l " +
									"join fetch l.rootEntity",
							RootEntity.class
					).uniqueResult();
					assertTrue( Hibernate.isInitialized( result.getIntermediateComponent().getLeaves() ) );
					assertThat( result.getIntermediateComponent().getLeaves().size(), is( 2 ) );

					assertThat( session.getSessionFactory().getStatistics().getPrepareStatementCount(), is( 1L ) );
				}
		);
	}

	@Entity( name = "RootEntity" )
	@Table( name = "root" )
	public static class RootEntity {
		private Integer id;
		private IntermediateComponent intermediateComponent;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Embedded
		public IntermediateComponent getIntermediateComponent() {
			return intermediateComponent;
		}

		public void setIntermediateComponent(IntermediateComponent intermediateComponent) {
			this.intermediateComponent = intermediateComponent;
		}
	}

	@Embeddable
	public static class IntermediateComponent {
		private String name;
		private Set<LeafEntity> leaves;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToMany( mappedBy = "rootEntity" )
		public Set<LeafEntity> getLeaves() {
			return leaves;
		}

		public void setLeaves(Set<LeafEntity> leaves) {
			this.leaves = leaves;
		}
	}

	@Entity( name = "LeafEntity" )
	@Table( name = "leaf" )
	public static class LeafEntity {
		private Integer id;
		private String name;
		private RootEntity rootEntity;

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne
		@JoinColumn
		public RootEntity getRootEntity() {
			return rootEntity;
		}

		public void setRootEntity(RootEntity rootEntity) {
			this.rootEntity = rootEntity;
		}
	}
}
