/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.readonly;

import java.util.Objects;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.MutableMutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexander Schwartz
 * @author Jan Schatteman
 */
@DomainModel(
		annotatedClasses = { ReadOnlyUserDefinedTest.MyEntity.class }
)
@SessionFactory
public class ReadOnlyUserDefinedTest {

	@Test
	public void test(SessionFactoryScope scope) {
		Long id = scope.fromTransaction(
				session -> {
					MyEntity myEntity = new MyEntity();
					MyType myType = new MyType();
					myType.setMutableState("A");
					myEntity.setMyType(myType);
					session.persist( myEntity );
					return myEntity.getId();
				}
		);

		scope.inTransaction(
				session -> {
					session.setDefaultReadOnly(true);
					MyEntity myEntity = session.find(MyEntity.class, id);
					assertEquals("A", myEntity.getMyType().getMutableState());

					// BUG: when calling "setReadOnly(..., false)" this misses to clone the mutable type field
					session.setReadOnly( myEntity, false );

					myEntity.getMyType().setMutableState("B");
					assertEquals("B", myEntity.getMyType().getMutableState());
				}
		);

		scope.inTransaction(
				session -> {
					session.setDefaultReadOnly(true);
					MyEntity myEntity = session.find(MyEntity.class, id);
					assertEquals("B", myEntity.getMyType().getMutableState());
				}
		);
	}

	@Entity
	@Table(name = "MYTABLE")
	public static class MyEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@org.hibernate.annotations.JavaType( value = MyTypeJavaType.class )
		private MyType myType;

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}


		public MyType getMyType() {
			return myType;
		}

		public void setMyType(MyType myType) {
			this.myType = myType;
		}
	}

	public static class MyType {
		private String mutableState;

		public String getMutableState() {
			return mutableState;
		}

		public void setMutableState(String mutableState) {
			this.mutableState = mutableState;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MyType myType = (MyType) o;
			return mutableState.equals(myType.mutableState);
		}

		@Override
		public int hashCode() {
			return Objects.hash( mutableState);
		}
	}

	public static class MyTypeJavaType extends AbstractClassJavaType<MyType> {
		public static final MyTypeJavaType INSTANCE = new MyTypeJavaType();

		protected MyTypeJavaType() {
			super(MyType.class, new MutableMutabilityPlan<>() {
				@Override
				protected MyType deepCopyNotNull(MyType value) {
					MyType myType = new MyType();
					myType.setMutableState( value.getMutableState() );
					return myType;
				}
			});
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return indicators.getJdbcType( SqlTypes.VARCHAR );
		}

		@Override
		public MyType fromString(CharSequence cs) {
			MyType myType = new MyType();
			myType.setMutableState(cs.toString());
			return myType;
		}

		@Override
		public <X> X unwrap(MyType value, Class<X> type, WrapperOptions options) {
			return (X) value.getMutableState();
		}

		@Override
		public <X> MyType wrap(X value, WrapperOptions options) {
			MyType myType = new MyType();
			myType.setMutableState((String) value);
			return myType;
		}
	}

}
