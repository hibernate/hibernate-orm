/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.uuid.strategy;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.dialect.SybaseDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Steve Ebersole
 * @author Nathan Xu
 */
@DomainModel(
		annotatedClasses = CustomStrategyTest.Node.class
)
@SessionFactory
@SkipForDialect( dialectClass = SybaseDialect.class, matchSubTypes = true,
		reason = "Skipped for Sybase to avoid problems with UUIDs potentially ending with a trailing 0 byte")
public class CustomStrategyTest {

	@Test
	public void testUsage(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Node node = new Node();
			session.persist( node );
			assertThat(node.id, notNullValue());
			assertThat(node.id.variant(), is(2));
			assertThat(node.id.version(), is(1));
		} );
	}

	@Entity(name = "Node")
	static class Node {

		@Id
		@GeneratedValue( generator = "custom-uuid" )
		@GenericGenerator(
				name = "custom-uuid",
				strategy = "org.hibernate.id.UUIDGenerator",
				parameters = {
						@Parameter(
								name = "uuid_gen_strategy_class",
								value = "org.hibernate.id.uuid.CustomVersionOneStrategy"
						)
				}
		)
		UUID id;

		String name;
	}
}
