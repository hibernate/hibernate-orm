/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
public class EntityManagerDeserializationTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class};
	}

	@Test(expected = PersistenceException.class)
	@TestForIssue(jiraKey = "HHH-11555")
	public void deserializedEntityManagerPersistenceExceptionManagementTest() throws IOException {
		EntityManager entityManager = getOrCreateEntityManager();
		final EntityManager deserializedSession = spoofSerialization( entityManager );
		try {
			deserializedSession.getTransaction().begin();
			TestEntity entity = new TestEntity();
			entity.setName( "Andrea" );
			deserializedSession.persist( entity );
			entity.setName( null );
			deserializedSession.flush();
		}
		finally {
			deserializedSession.getTransaction().rollback();
			deserializedSession.close();
			entityManager.close();
		}
	}

	private EntityManager spoofSerialization(EntityManager entityManager) throws IOException {
		try {
			// Serialize the incoming out to memory
			ByteArrayOutputStream serBaOut = new ByteArrayOutputStream();
			ObjectOutputStream serOut = new ObjectOutputStream( serBaOut );

			serOut.writeObject( entityManager );

			// Now, re-constitute the model from memory
			ByteArrayInputStream serBaIn =
					new ByteArrayInputStream( serBaOut.toByteArray() );
			ObjectInputStream serIn = new ObjectInputStream( serBaIn );

			EntityManager outgoing = (EntityManager) serIn.readObject();

			return outgoing;
		}
		catch (ClassNotFoundException cnfe) {
			throw new IOException( "Unable to locate class on reconstruction" );
		}
	}

	@Entity
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column(nullable = false)
		String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
