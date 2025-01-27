/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.inheritance;

import jakarta.persistence.*;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(annotatedClasses = {
		EmbeddableInheritanceReplaceTest.Emb.class,
		EmbeddableInheritanceReplaceTest.Base.class,
		EmbeddableInheritanceReplaceTest.Next.class,
		EmbeddableInheritanceReplaceTest.Ent.class
})
@SessionFactory
@JiraKey("HHH-19079")
public class EmbeddableInheritanceReplaceTest {

	@Test
	void merge(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			final var history = new Ent();
			history.setBase(new Emb(42, "Hello, World!"));

			session.merge(history);
		});
	}

	@Embeddable
	@DiscriminatorValue("E")
	public static final class Emb extends Next {

		Emb() {
		}

		public Emb(int num, String str) {
			super(num, str);
		}
	}

	@Embeddable
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.CHAR)
	@DiscriminatorValue("b")
	public static class Base {

		protected int num;

		protected Base() {
		}

		public Base(int num, String str) {
			this.num = num;
		}

		public int getNum() {
			return num;
		}

		public void setNum(int num) {
			this.num = num;
		}
	}

	@Embeddable
	@DiscriminatorValue("n")
	public static class Next extends Base {

		private String str;

		public Next(int num, String str) {
			super(num, str);
			this.str = str;
		}

		public Next() {
		}
	}

	@Entity(name = "Ent")
	public static class Ent {

		@Id
		@GeneratedValue
		private Integer id;

		@Embedded
		private Base base;

		public Ent() {
		}

		public Ent(final Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Base getBase() {
			return base;
		}

		public void setBase(Base base) {
			this.base = base;
		}
	}
}
