/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.records;

import java.time.LocalDate;

import org.hibernate.annotations.EmbeddableInstantiator;
import org.hibernate.metamodel.spi.ValueAccess;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				EmbeddableInstantiatorTest.Wrapper.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-16457")
public class EmbeddableInstantiatorTest {

	@Test
	public void writesAndReadsInstantiatedEmbeddable(SessionFactoryScope scope) {

		Wrapper wrapper = new Wrapper();
		LocalDate aValue = LocalDate.now();
		String bValue = "Foo";
		int cValue = 1;

		wrapper.myRecord = new MyRecord( cValue, bValue, aValue );

		scope.inTransaction(
				session -> {
					session.persist( wrapper );
				}
		);

		scope.inTransaction(
				session -> {
					Wrapper result = session.find( Wrapper.class, wrapper.id );
					assertThat( result ).isNotNull();

					assertThat(result.id).isEqualTo( cValue );

					MyRecord myRecord = result.myRecord;
					assertThat( myRecord ).isNotNull();

					assertThat(myRecord.c).isEqualTo( cValue );
					assertThat(myRecord.b).isEqualTo( bValue );
					assertThat(myRecord.a).isEqualTo( aValue );
				}
		);
	}

	@Embeddable
	@EmbeddableInstantiator(MyRecord.MyRecordInstantiator.class)
	public record MyRecord(int c, String b, LocalDate a) {

		public static class MyRecordInstantiator implements org.hibernate.metamodel.spi.EmbeddableInstantiator {

			@Override
			public boolean isInstance(Object object) {
				return MyRecord.class.isInstance( object );
			}

			@Override
			public boolean isSameClass(Object object) {
				return MyRecord.class.equals( object.getClass() );
			}

			@Override
			public Object instantiate(ValueAccess valueAccess) {
				return new MyRecord( valueAccess.getValue( 2, Integer.class ), valueAccess.getValue( 1, String.class ),
									valueAccess.getValue( 0, LocalDate.class )
				);
			}
		}
	}

	@Entity
	public static class Wrapper {

		@Id
		@GeneratedValue
		Long id;

		MyRecord myRecord;
	}
}
