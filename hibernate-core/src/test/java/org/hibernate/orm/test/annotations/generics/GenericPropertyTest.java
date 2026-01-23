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
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.sql.Types;

@DomainModel(
		annotatedClasses = {
				GenericPropertyTest.GenericEntity.class,
				GenericPropertyTest.StringGenericEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-20101")
public class GenericPropertyTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void smoke(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			GenericEntity<String> genericEntity = new StringGenericEntity();
			genericEntity.setId( 2 );
			genericEntity.setContent( "genericEntityContent" );
			genericEntity.setArrayContent( new String[] {"entry1", "entry2"} );

			session.persist( genericEntity );
		} );
	}

@Entity(name = "generic")
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

@Entity(name = "stringgeneric")
public static class StringGenericEntity extends GenericEntity<String> {

}
}
