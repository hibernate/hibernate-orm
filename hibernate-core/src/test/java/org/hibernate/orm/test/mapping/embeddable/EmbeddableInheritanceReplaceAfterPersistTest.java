/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable;


import jakarta.persistence.*;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = {
		EmbeddableInheritanceReplaceAfterPersistTest.Emb.class,
		EmbeddableInheritanceReplaceAfterPersistTest.Base.class,
		EmbeddableInheritanceReplaceAfterPersistTest.Next.class,
		EmbeddableInheritanceReplaceAfterPersistTest.Ent.class
} )
@SessionFactory
@JiraKey( "HHH-20125" )
public class EmbeddableInheritanceReplaceAfterPersistTest {

	@Test
	void merge(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var history = new Ent();
			history.setBase( new Emb( 42, "Hello, World!" ) );
			session.persist( history );
			session.flush();
			session.clear();

			history.setBase( new Base( 43 ) );
			var merged = session.merge( history );

			assertThat( merged.getBase() )
					.isInstanceOfSatisfying(
							Base.class, next -> {
								assertThat( next.getNum() ).isEqualTo( 43 );
							}
					);
		} );
	}

	@Embeddable
	@DiscriminatorColumn( discriminatorType = DiscriminatorType.CHAR )
	@DiscriminatorValue( "b" )
	public static class Base {

		protected int num;

		protected Base() {
		}

		public Base(int num) {
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
	@DiscriminatorValue( "n" )
	public static class Next extends Base {

		private String str;

		public Next(int num, String str) {
			super( num );
			this.str = str;
		}

		public Next() {
		}

		public String getStr() {
			return str;
		}
	}

	@Embeddable
	@DiscriminatorValue( "E" )
	public static final class Emb extends Next {

		Emb() {
		}

		public Emb(int num, String str) {
			super( num, str );
		}
	}

	@Entity( name = "Ent" )
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
