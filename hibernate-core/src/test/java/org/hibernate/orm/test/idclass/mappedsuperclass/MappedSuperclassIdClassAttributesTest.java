/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idclass.mappedsuperclass;

import jakarta.persistence.metamodel.SingularAttribute;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {Summary.class, BaseSummary.class})
@SessionFactory
@JiraKey("HHH-18858")
public class MappedSuperclassIdClassAttributesTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inSession( entityManager -> {
			final var yearAttribute = Summary_.year.getDeclaringType().getAttribute( "year" );
			assertThat( yearAttribute ).isEqualTo( Summary_.year );
			assertThat( ((SingularAttribute<?, ?>) yearAttribute).isId() ).isTrue();

			final var monthAttribute = Summary_.month.getDeclaringType().getAttribute( "month" );
			assertThat( monthAttribute ).isEqualTo( Summary_.month );
			assertThat( ((SingularAttribute<?, ?>) monthAttribute).isId() ).isTrue();
		} );
	}
}
