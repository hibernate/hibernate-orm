/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.sequence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = OptimizerTest.TheEntity.class
)
@SessionFactory
public class OptimizerTest {

	@Test
	@JiraKey(value = "HHH-10166")
	public void testGenerationPastBound(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 100; i++ ) {
						TheEntity entity = new TheEntity( Integer.toString( i ) );
						session.persist( entity );
					}
				}
		);

		scope.inTransaction(
				session -> {
					TheEntity number100 = session.get( TheEntity.class, 100 );
					assertThat( number100, notNullValue() );
					session.createQuery( "delete TheEntity" ).executeUpdate();
				}
		);
	}

	@Entity(name = "TheEntity")
	@Table(name = "TheEntity")
	public static class TheEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq1")
		@SequenceGenerator(name = "seq1", sequenceName = "the_sequence")
		public Integer id;
		public String someString;

		public TheEntity() {
		}

		public TheEntity(String someString) {
			this.someString = someString;
		}
	}
}
