/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetomany;

import java.util.Set;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PersistenceException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DomainModel(
		annotatedClasses = {
				OneToManyJoinColumnsUniquenessTest.EntityA.class,
				OneToManyJoinColumnsUniquenessTest.EntityB.class,
		}
)
@SessionFactory(useCollectingStatementInspector = true)
@JiraKey(value = "HHH-15091")
public class OneToManyJoinColumnsUniquenessTest {

	@Test
	public void testInsertWithNullAssociationThrowPersistenceException(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		assertThrows(
				PersistenceException.class, () ->
						scope.inTransaction(
								session -> {
									EntityB entityB = new EntityB( 1l );
									session.persist( entityB );
								}
						)
		);
		// check that no insert statement has bees executed
		statementInspector.assertExecutedCount( 0 );
	}

	@Entity(name = "EntityA")
	public static class EntityA {

		@EmbeddedId
		private PK id;

		@OneToMany(mappedBy = "entityA", fetch = FetchType.LAZY)
		private Set<EntityB> entityBs;

		public EntityA() {
		}
	}

	@Embeddable
	public static class PK {
		@Column(name = "id_1")
		private String id1;
		@Column(name = "id_2")
		private String id2;

		public PK() {
		}

		public PK(String id1, String id2) {
			this.id1 = id1;
			this.id2 = id2;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Long id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "b_to_a_1", referencedColumnName = "id_1", nullable = false)
		@JoinColumn(name = "b_to_a_2", referencedColumnName = "id_2", nullable = false)
		private EntityA entityA;

		public EntityB() {
		}

		public EntityB(Long id) {
			this.id = id;
		}
	}
}
