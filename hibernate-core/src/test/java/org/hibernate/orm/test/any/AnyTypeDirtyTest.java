/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Expected behavior:
 * <p>
 * Hibernate must not consider an {@link org.hibernate.annotations.Any} association
 * dirty when the referenced entity has not logically changed.
 * </p>
 *
 * <p>
 * In particular, replacing a proxy obtained via
 * {@link jakarta.persistence.EntityManager#getReference(Class, Object)} with its
 * underlying implementation instance must not trigger dirty checking, as both
 * references represent the same persistent entity.
 * </p>
 *
 * <p>
 * The presence of a proxy versus a non-proxy instance is an implementation detail
 * and must not result in an SQL {@code UPDATE} during flush.
 * </p>
 *
 * <p>
 * This test asserts that no {@code UPDATE} statements are executed when flushing
 * the persistence context, using a {@code SQLStatementInspector}.
 * </p>
 *
 * @author Vincent Bouthinon
 * @author Utsav Mehta
 * @see org.hibernate.annotations.Any
 * @see jakarta.persistence.EntityManager#getReference(Class, Object)
 * @see org.hibernate.testing.jdbc.SQLStatementInspector
 */
@Jpa(annotatedClasses = {
		AnyTypeDirtyTest.Formulaire.class,
		AnyTypeDirtyTest.Operation.class},
		useCollectingStatementInspector = true)
@JiraKey("HHH-20020")
public class AnyTypeDirtyTest {

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory()
				.unwrap( SessionFactory.class)
				.getSchemaManager()
				.truncateMappedObjects();
	}

	@Test
	void isNotDirty(EntityManagerFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();

		Long idFormulaire = scope.fromTransaction( entityManager -> {
			Formulaire formulaire = new Formulaire();
			formulaire.addToOperations( new Operation() );
			entityManager.persist( formulaire );
			entityManager.flush();
			entityManager.clear();
			return formulaire.getId();
		} );

		sql.clear();

		scope.inTransaction( entityManager -> {
			Formulaire formulaireProxy = entityManager.getReference( Formulaire.class, idFormulaire );
			formulaireProxy.addToOperations( new Operation() );
			entityManager.merge( formulaireProxy );
			entityManager.flush();
		} );

		sql.assertNoUpdate();

		scope.inTransaction( entityManager -> {
			Formulaire reloaded =
					entityManager.find( Formulaire.class, idFormulaire );
			assertEquals( 2, reloaded.operations.size() );
			for ( Operation op : reloaded.operations ) {
				assertEquals( reloaded, op.getFormulaire(),
						"The @Any association should still point to the correct Formulaire" );
			}
		} );
	}

	@Test
	void isDirtyWhenAssociationChanges(EntityManagerFactoryScope scope) {
		final SQLStatementInspector sql = scope.getCollectingStatementInspector();

		Long id1 = scope.fromTransaction( entityManager -> {
			Formulaire f1 = new Formulaire();
			entityManager.persist( f1 );
			return f1.getId();
		} );

		Long id2 = scope.fromTransaction( entityManager -> {
			Formulaire f2 = new Formulaire();
			entityManager.persist( f2 );
			return f2.getId();
		} );

		Long opId = scope.fromTransaction( entityManager -> {
			Operation op = new Operation();
			op.setFormulaire( entityManager.getReference( Formulaire.class, id1 ) );
			entityManager.persist( op );
			entityManager.flush();

			sql.clear();

			op.setFormulaire( entityManager.getReference( Formulaire.class, id2 ) );
			entityManager.flush();
			return op.id;
		} );

		sql.assertUpdate();

		scope.inTransaction( entityManager -> {
			Operation reloaded =
					entityManager.find( Operation.class, opId );

			Formulaire associated = (Formulaire) reloaded.getFormulaire();
			assertEquals( id2, associated.getId(),
					"The @Any association should now point to the updated Formulaire" );
		} );
	}

	@Entity(name = "formulaire")
	@Table(name = "formulaire")
	public static class Formulaire {

		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(cascade = ALL, fetch = LAZY)
		@Fetch(FetchMode.SUBSELECT)
		protected Set<Operation> operations = new HashSet<>();

		public boolean addToOperations(Operation operation) {
			operation.setFormulaire( this );
			return operations.add( operation );
		}
		public Long getId() {
			return id;
		}
	}

	@Entity(name = "operation")
	@Table(name = "toperation")
	public static class Operation {

		@Id
		@GeneratedValue
		private Long id;

		@Any(fetch = LAZY)
		@AnyKeyJavaClass(Long.class)
		@JoinColumn(name = "FORMULAIRE_ID")
		@Column(name = "FORMULAIRE_ROLE")
		private Object formulaire;

		public Object getFormulaire() {
			return formulaire;
		}

		public void setFormulaire(Object formulaire) {
			this.formulaire = formulaire;
		}
	}
}
