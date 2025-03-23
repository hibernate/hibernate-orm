/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.embeddable.strategy.usertype.embedded.merge;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = { Parent.class, Child.class }
)
@SessionFactory
@JiraKey(value = "HHH-16015")
public class MergeTest {

	@Test
	public void testMerge(SessionFactoryScope scope) {
		final Parent parent = new Parent( 1l, "Lio" );
		scope.inTransaction(
				session -> {
					Child child = new Child( new MyCompositeValue( 2l, "initial value" ) );
					parent.setChild( child );
					session.persist( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Child child = new Child( new MyCompositeValue( 1l, "updated value" ) );
					parent.setChild( child );

					session.merge( parent );
				}
		);

		scope.inTransaction(
				session -> {
					Parent lio = session.get( Parent.class, 1 );
					Child child = lio.getChild();

					assertThat( child ).isNotNull();
					MyCompositeValue compositeValue = child.getCompositeValue();
					assertThat( compositeValue ).isNotNull();
					assertThat( compositeValue.stringValue() ).isEqualTo( "updated value" );
				}
		);
	}
}
