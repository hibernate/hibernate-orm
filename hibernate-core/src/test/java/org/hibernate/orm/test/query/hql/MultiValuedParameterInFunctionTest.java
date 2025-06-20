/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.Collections;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@BootstrapServiceRegistry(
		javaServices = {
				@JavaService(role = FunctionContributor.class, impl = MultiValuedParameterInFunctionTest.MyFunctionContributor.class)
		}
)
@DomainModel(annotatedClasses = MultiValuedParameterInFunctionTest.Post.class)
@SessionFactory
@RequiresDialect(H2Dialect.class)
public class MultiValuedParameterInFunctionTest {

	@Test
	@JiraKey( "HHH-16787" )
	public void testAsQueryParameter(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "from Post p where my_function((select p2.id from Post p2 where p2.title in (:name)))=1" )
					.setParameter( "name", Collections.singletonList( "1" ) )
					.list();
		} );
	}


	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}


	@Entity(name = "Post")
	@Table(name = "Post")
	public static class Post {
		@Id
		public Integer id;
		@Basic
		public String title;

		private Post() {
			// for Hibernate use
		}

		public Post(Integer id, String title) {
			this.id = id;
			this.title = title;
		}
	}
	public static class MyFunctionContributor implements FunctionContributor {

		@Override
		public void contributeFunctions(FunctionContributions functionContributions) {
			functionContributions.getFunctionRegistry().registerPattern(
					"my_function",
					"?1"
			);
		}
	}
}
