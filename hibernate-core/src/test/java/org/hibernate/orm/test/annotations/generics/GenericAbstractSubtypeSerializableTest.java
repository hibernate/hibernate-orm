/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.generics;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		GenericAbstractSubtypeSerializableTest.GenericEntity.class,
		GenericAbstractSubtypeSerializableTest.StringEntity.class,
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20101")
public class GenericAbstractSubtypeSerializableTest {

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void testStringEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			GenericEntity<String> genericEntity = new StringEntity();
			genericEntity.setId( 1 );
			genericEntity.setContent( "string_entity" );
			genericEntity.setArrayContent( new String[] {"entry1", "entry2"} );
			session.persist( genericEntity );
		} );
		scope.inSession( session -> {
			GenericEntity<?> retrieved = session.find( GenericEntity.class, 1 );
			assertThat( retrieved.getContent() ).isEqualTo( "string_entity" );
			assertThat( (Object[]) retrieved.getArrayContent() ).containsExactly( "entry1", "entry2" );
		} );
	}

	@Entity(name = "GenericEntity")
	public abstract static class GenericEntity<T extends Serializable> {
		@Id
		private Integer id;

		@Basic
		private T content;

		@Basic
		@JdbcTypeCode(Types.VARBINARY)
		private T[] arrayContent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public T getContent() {
			return content;
		}

		public void setContent(T content) {
			this.content = content;
		}

		public T[] getArrayContent() {
			return arrayContent;
		}

		public void setArrayContent(T[] arrayContent) {
			this.arrayContent = arrayContent;
		}
	}

	@Entity(name = "StringEntity")
	static class StringEntity extends GenericEntity<String> {
	}
}
