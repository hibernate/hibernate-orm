/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.refcolnames.secondary;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@JiraKey(value = "HHH-15933")
@SessionFactory
@DomainModel(annotatedClasses = { Split.class, Reference.class })
public class RefToSecondaryTableTest {
	@Test
	public void test(SessionFactoryScope scope) {
		Split split = new Split();
		split.setCode( 123 );
		split.setDescription( "blah" );
		split.setName( "Split" );
		Reference reference = new Reference();
		reference.setSplit( split );
		scope.inTransaction( session -> {
			session.persist( split );
			session.persist( reference );
		} );
		scope.inSession( session -> {
			Reference ref =
					session.createQuery( "from Reference left join fetch split", Reference.class )
							.getSingleResult();
			Assertions.assertEquals( split.getId(), ref.getSplit().getId() );
		} );
		scope.inSession( session -> {
			Reference ref = session.find( Reference.class, reference.getId() );
			Assertions.assertEquals( split.getId(), ref.getSplit().getId() );
		} );
	}
}
