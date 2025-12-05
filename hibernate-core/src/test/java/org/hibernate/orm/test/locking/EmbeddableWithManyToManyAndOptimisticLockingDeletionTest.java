/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import java.util.Set;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.annotations.OptimisticLocking;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.annotations.CacheConcurrencyStrategy.READ_WRITE;


@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				EmbeddableWithManyToManyAndOptimisticLockingDeletionTest.TagOperator.class,
				EmbeddableWithManyToManyAndOptimisticLockingDeletionTest.Tag.class
		}
)
@SessionFactory
@JiraKey("HHH-16821")
public class EmbeddableWithManyToManyAndOptimisticLockingDeletionTest {
	private static final String TAG_OPERATOR_ID = "tagOperatorID";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TagOperator operator = new TagOperator( TAG_OPERATOR_ID );
			session.persist( operator );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testDelete(SessionFactoryScope scope){
		scope.inTransaction( session -> {
			TagOperator tagOperator = session.find( TagOperator.class, TAG_OPERATOR_ID );
			assertThat(tagOperator).isNotNull();

			session.remove( tagOperator );
		} );
	}

	@Entity(name = "TagOperator")
	@Table(name = "TAG_OPERATOR")
	@OptimisticLocking(type = OptimisticLockType.DIRTY)
	@DynamicUpdate
	@Cacheable
	public static class TagOperator {

		public TagOperator() {
		}

		public TagOperator(String operatorId) {
			this.operatorId = operatorId;
			this.tagPolicy = new OperatorTagPolicy( Set.of(), PolicyType.ALL );
		}

		@Id
		@Column(name = "OPERATOR_ID", nullable = false)
		private String operatorId;

		@Embedded
		private OperatorTagPolicy tagPolicy;
	}

	@Embeddable
	public static class OperatorTagPolicy {
		@ManyToMany(fetch = LAZY)
		@JoinTable(
				name = "OPERATOR_TO_TAG",
				joinColumns = {
						@JoinColumn(name = "OPERATOR_ID", referencedColumnName = "OPERATOR_ID")
				},
				inverseJoinColumns = @JoinColumn(name = "TAG_NAME"))
		@org.hibernate.annotations.Cache(usage = READ_WRITE)
		Set<Tag> exceptions;

		@Column(name = "TAG_POLICY_TYPE", nullable = false)
		@Enumerated(EnumType.STRING)
		PolicyType type;

		public OperatorTagPolicy() {
		}

		public OperatorTagPolicy(Set<Tag> exceptions, PolicyType type) {
			this.exceptions = exceptions;
			this.type = type;
		}

		public Set<Tag> getExceptions() {
			return exceptions;
		}

		public PolicyType getType() {
			return type;
		}
	}

	@Entity(name = "Tag")
	@Table(name = "TAG_TABLE")
	public static class Tag {
		@Id
		@Column(name = "TAG_NAME", nullable = false)
		private String name;

		public Tag() {
		}

		public Tag(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	public enum PolicyType {
		ALL,
		NONE
	}
}
