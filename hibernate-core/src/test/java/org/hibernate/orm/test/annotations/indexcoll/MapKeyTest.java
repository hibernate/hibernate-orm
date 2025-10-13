/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.indexcoll;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				GenerationUser.class,
				GenerationGroup.class
		}
)
@SessionFactory
public class MapKeyTest {

	@Test
	public void testMapKeyOnEmbeddedId(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Generation c = new Generation();
					c.setAge( "a" );
					c.setCulture( "b" );
					c.setSubGeneration( new Generation.SubGeneration( "description" ) );
					GenerationGroup r = new GenerationGroup();
					r.setGeneration( c );
					session.persist( r );
					GenerationUser m = new GenerationUser();
					session.persist( m );
					m.getRef().put( c, r );
					session.flush();
					session.clear();

					m = session.find( GenerationUser.class, m.getId() );
					Generation cRead = m.getRef().keySet().iterator().next();
					assertThat( cRead.getAge() ).isEqualTo( "a" );
					assertThat( cRead.getSubGeneration().getDescription() ).isEqualTo( "description" );
				}
		);
	}
}
