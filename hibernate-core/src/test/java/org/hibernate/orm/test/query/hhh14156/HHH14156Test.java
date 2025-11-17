/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hhh14156;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

/**
 * @author Nathan Xu
 * @author Christian Beikov
 */
@JiraKey( value = "HHH-14156" )
@DomainModel(
		annotatedClasses = {
				HHH14156Test.EntityWithCompositeId.class
		}
)
@SessionFactory
public class HHH14156Test{

	@Test
	public void testNoExceptionThrown(SessionFactoryScope scope) {
		scope.inTransaction( session ->
			session.createQuery(
					"from EntityWithCompositeId e where e in (select e2 from EntityWithCompositeId e2)",
					EntityWithCompositeId.class
			).getResultList()
		);
	}

	@Entity(name = "EntityWithCompositeId")
	@Table(name = "EntityWithCompositeId")
	public static class EntityWithCompositeId implements Serializable {

		@EmbeddedId
		private PK id;

		@Embeddable
		public static class PK implements Serializable {

			private String id1;

			private String id2;

			public PK(String id1, String id2) {
				this.id1 = id1;
				this.id2 = id2;
			}

			private PK() {
			}

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( o == null || getClass() != o.getClass() ) {
					return false;
				}
				PK pk = (PK) o;
				return Objects.equals( id1, pk.id1 ) &&
						Objects.equals( id2, pk.id2 );
			}

			@Override
			public int hashCode() {
				return Objects.hash( id1, id2 );
			}
		}
	}
}
