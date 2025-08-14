/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@DomainModel(annotatedClasses = {
		NativeQueryResultBuilderColumnDeduplicationTest.MyEntity.class
})
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-19712")
public class NativeQueryResultBuilderColumnDeduplicationTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createNativeQuery( "select {t.*} from MyEntity t", Object.class )
							.addEntity( "t", MyEntity.class )
							.getResultList();
				}
		);
	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@EmbeddedId
		private MyEntityPk id;
		@Column(insertable = false, updatable = false)
		private String name;
		private String description;
	}

	@Embeddable
	public static class MyEntityPk {
		private String id;
		private String name;
	}
}
