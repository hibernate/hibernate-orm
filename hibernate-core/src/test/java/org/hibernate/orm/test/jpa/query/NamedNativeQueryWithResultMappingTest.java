/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityResult;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.Table;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

@SessionFactory
@DomainModel(annotatedClasses = NamedNativeQueryWithResultMappingTest.Mapped.class)
@Jira( "https://hibernate.atlassian.net/browse/HHH-18537" )
public class NamedNativeQueryWithResultMappingTest {
	@Test void test(SessionFactoryScope scope) {
		Mapped mapped = new Mapped();
		mapped.name = "Gavin";
		scope.inTransaction(s -> s.persist(mapped));
		scope.inSession(s -> {
			s.createNamedSelectionQuery("mapped-native-query",Object[].class).getSingleResult();
			s.createNamedSelectionQuery("unmapped-native-query",Object[].class).getSingleResult();
		});
	}
	@NamedNativeQuery(
			name = "mapped-native-query",
			query = "select id, name, 1 as one, 'hello' as hello from mapped",
			entities = @EntityResult(entityClass = Mapped.class),
			columns = {@ColumnResult(name = "one", type = Integer.class),
					@ColumnResult(name = "hello", type = String.class)}
	)
	@NamedNativeQuery(
			name = "unmapped-native-query",
			query = "select id, name, 1 as one from mapped",
			classes = @ConstructorResult(targetClass = Unmapped.class,
					columns = {@ColumnResult(name = "name"),
							@ColumnResult(name = "id"),
							@ColumnResult(name = "one")})
	)
	@Entity(name = "Mapped")
	@Table(name = "mapped")
	static class Mapped {
		@Id @GeneratedValue
		Long id;
		String name;
	}
	record Unmapped(String name, long id, int one) {}
}
