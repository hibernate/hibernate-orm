package org.hibernate.query.hhh14156;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Nathan Xu
 * @author Christian Beikov
 */
@TestForIssue( jiraKey = "HHH-14156" )
public class HHH14156Test extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityWithCompositeId.class };
	}

	@Test
	@SkipForDialect(value = SQLServerDialect.class, comment = "SQLServer doesn't support tuple comparisons")
	@SkipForDialect(value = DerbyDialect.class, comment = "Derby doesn't support tuple comparisons")
	public void testNoExceptionThrown() {
		inTransaction( session ->
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
