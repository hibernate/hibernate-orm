/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@DomainModel(
		annotatedClasses = { EmbeddableAndGenericMappedSuperclassTest.MyEntity.class }
)
@SessionFactory
public class EmbeddableAndGenericMappedSuperclassTest {

	@JiraKey("HHH-17041")
	@Test
	public void testQueryEmbeddableFields(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
						session.createQuery( "select m.embedded.text, m.embedded.name from MyEntity m" ).list();
				}
		);
	}

	@MappedSuperclass
	public static abstract class MyAbstractEmbeddable {
		@Column
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}


	@Embeddable
	public static class MyEmbeddable extends MyAbstractEmbeddable {

		@Column
		private String text;

		public MyEmbeddable() {
		}

		private MyEmbeddable(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@MappedSuperclass
	public static abstract class MyMappedSuperclass<C extends MyAbstractEmbeddable> {
		@Embedded
		private C embedded;

		public C getEmbedded() {
			return embedded;
		}

		public void setEmbedded(C embedded) {
			this.embedded = embedded;
		}
	}

	@Entity(name = "MyEntity")
	public static class MyEntity extends MyMappedSuperclass<MyEmbeddable> {

		@Id
		private Integer id;

		public MyEntity() {
		}

		private MyEntity(Integer id, String text) {
			this.id = id;
			setEmbedded( new MyEmbeddable( text ) );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

	}

}
