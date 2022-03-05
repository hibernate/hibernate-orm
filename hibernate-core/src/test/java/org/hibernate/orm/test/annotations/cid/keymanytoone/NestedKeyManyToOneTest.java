/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.cid.keymanytoone;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

/**
 * @author Christian Beikov
 */
@DomainModel(
		annotatedClasses = {
				NestedKeyManyToOneTest.BasicEntity.class, NestedKeyManyToOneTest.IdClassEntity.class, NestedKeyManyToOneTest.NestedIdClassEntity.class
		})
@SessionFactory
public class NestedKeyManyToOneTest {

	@Test
	public void testNestedIdClassAssociations(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "SELECT idClassEntity_1.basicEntity.key1 FROM NestedIdClassEntity a JOIN a.idClassEntity idClassEntity_1" ).getResultList();
				}
		);
	}

	@Entity(name = "BasicEntity")
	public static class BasicEntity {
		@Id
		Long key1;
	}

	@Entity(name = "IdClassEntity")
	@IdClass(IdClassEntity.IdClassEntityId.class)
	public static class IdClassEntity {
		@Id
		@ManyToOne
		BasicEntity basicEntity;
		@Id
		Long key2;

		public static class IdClassEntityId implements Serializable {
			Long basicEntity;
			Long key2;
		}
	}

	@Entity(name = "NestedIdClassEntity")
	@IdClass(NestedIdClassEntity.NestedIdClassEntityId.class)
	public static class NestedIdClassEntity {
		@Id
		@ManyToOne
		IdClassEntity idClassEntity;
		@Id
		Long key3;

		public static class NestedIdClassEntityId implements Serializable {
			IdClassEntity.IdClassEntityId idClassEntity;
			Long key3;
		}
	}

}
