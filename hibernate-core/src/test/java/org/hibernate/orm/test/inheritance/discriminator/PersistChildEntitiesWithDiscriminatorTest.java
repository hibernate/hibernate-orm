/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.inheritance.discriminator;

import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.orm.test.mapping.inheritance.discriminator.InheritingEntity;
import org.hibernate.orm.test.mapping.inheritance.discriminator.ParentEntity;
import org.junit.jupiter.api.Test;

/**
 * @author Pawel Stawicki
 */
@SessionFactory
@RequiresDialect(value = PostgreSQLDialect.class)
@DomainModel(annotatedClasses = {
		ParentEntity.class, InheritingEntity.class
})
@JiraKey(value = "HHH-6580")
public class PersistChildEntitiesWithDiscriminatorTest {

	@Test
	public void doIt(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// we need the 2 inserts so that the id is incremented on the second get-generated-keys-result set, since
					// on the first insert both the pk and the discriminator values are 1
					session.persist( new InheritingEntity( "yabba" ) );
					session.persist( new InheritingEntity( "dabba" ) );
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "delete ParentEntity", null ).executeUpdate();

				}
		);
	}

}
