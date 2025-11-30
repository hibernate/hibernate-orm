/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.*;
import org.hibernate.annotations.JavaType;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.testing.orm.junit.*;
import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.BasicType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.jdbc.DateJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@JiraKey("HHH-18898")
@DomainModel(
		annotatedClasses = {
				EmbeddableWithJavaTypeTest.EntityEmbedCustom.class,
				EmbeddableWithJavaTypeTest.EntityEmbedNative.class
		}
)
@SessionFactory
class EmbeddableWithJavaTypeTest implements SessionFactoryScopeAware {

	private SessionFactoryScope scope;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	// uses an embeddable with a custom java type
	@ParameterizedTest
	@ValueSource(strings = {
			"select z from EntityEmbedCustom z where embedCustom.value=:datum",
			"select z from EntityEmbedCustom z where :datum=embedCustom.value",
			"select z from EntityEmbedCustom z where embedCustom.value=:datum", // this query failed with the bug
			"select z from EntityEmbedCustom z where :datum=embedCustom.value",
			"select z from EntityEmbedCustom z where embedCustom.value in (:datum)",
			"select z from EntityEmbedCustom z where embedCustom.value in (:datum)" // failed as well
	})
	void hhh18898Test_embedCustom(String hql) {

		// prepare
		scope.inTransaction( session -> {
			EntityEmbedCustom e = new EntityEmbedCustom();
			e.id = 1;
			EmbedCustom datum = new EmbedCustom();
			datum.value = new MyDate( LocalDate.now() );
			e.embedCustom = datum;
			session.persist( e );
		} );

		// assert
		scope.inTransaction( session -> {
			QueryImplementor<EntityEmbedCustom> query = session.createQuery( hql, EntityEmbedCustom.class );
			query.setParameter( "datum", new MyDate( LocalDate.now() ), MyDateJavaType.TYPE );
			List<EntityEmbedCustom> resultList = query.getResultList();
			assertFalse( resultList.isEmpty() );
			assertEquals( LocalDate.now(), resultList.get( 0 ).embedCustom.value.wrapped );
			session.remove( resultList.get( 0 ) );
		} );
	}

	// uses an embeddable with a native java type
	@ParameterizedTest
	@ValueSource(strings = {
			"select z from EntityEmbedNative z where embedNative.value=:datum",
			"select z from EntityEmbedNative z where :datum=embedNative.value",
			"select z from EntityEmbedNative z where embedNative.value=:datum", // this query failed with the bug
			"select z from EntityEmbedNative z where :datum=embedNative.value",
			"select z from EntityEmbedNative z where embedNative.value in (:datum)",
			"select z from EntityEmbedNative z where embedNative.value in (:datum)" // failed as well
	})
	void hhh18898Test_embedSingle(String hql) {

		// prepare
		scope.inTransaction( session -> {
			EntityEmbedNative e = new EntityEmbedNative();
			e.id = 1;
			EmbedNative datum = new EmbedNative();
			datum.value = LocalDate.now();
			e.embedNative = datum;
			session.persist( e );
		} );

		// assert
		scope.inTransaction( session -> {
			QueryImplementor<EntityEmbedNative> query = session.createQuery( hql, EntityEmbedNative.class );
			query.setParameter( "datum", LocalDate.now(), LocalDateJavaType.INSTANCE.getJavaType() );
			List<EntityEmbedNative> resultList = query.getResultList();
			assertFalse( resultList.isEmpty() );
			assertEquals( LocalDate.now(), resultList.get( 0 ).embedNative.value );
			session.remove( resultList.get( 0 ) );
		} );
	}

	@Embeddable
	public static class EmbedCustom {

		@Column(name = "DATUM")
		@JavaType(MyDateJavaType.class)
		MyDate value;

	}

	@Entity(name = "EntityEmbedCustom")
	public static class EntityEmbedCustom {

		@Id
		@Column(name = "id")
		long id;

		@Embedded
		EmbedCustom embedCustom;
	}

	@Embeddable
	public static class EmbedNative {

		@Column(name = "DATUM")
		@JavaType(LocalDateJavaType.class)
		LocalDate value;
	}

	@Entity(name = "EntityEmbedNative")
	public static class EntityEmbedNative {

		@Id
		@Column(name = "id")
		long id;

		@Embedded
		EmbedNative embedNative;
	}

	public static class MyDate {
		private final LocalDate wrapped;

		public MyDate(LocalDate dateValue) {
			wrapped = dateValue;
		}

		public LocalDate toLocalDate() {
			return wrapped;
		}
	}

	public static class MyDateJavaType extends AbstractClassJavaType<MyDate> {
		private static final MyDateJavaType INSTANCE = new MyDateJavaType();
		public static final BasicType<MyDate> TYPE = new AbstractSingleColumnStandardBasicType<>( DateJdbcType.INSTANCE,
				INSTANCE ) {

			@Override
			public String getName() {
				return "MyDateJavaType";
			}
		};

		protected MyDateJavaType() {
			super( MyDate.class );
		}

		@Override
		public <X> X unwrap(final MyDate value, final Class<X> type, final WrapperOptions options) {
			LocalDate dateValue = (value == null ? null : value.toLocalDate());
			return LocalDateJavaType.INSTANCE.unwrap( dateValue, type, options );
		}

		@Override
		public <X> MyDate wrap(final X value, final WrapperOptions options) {
			if ( value instanceof MyDate ) {
				return (MyDate) value;
			}
			LocalDate dateValue = LocalDateJavaType.INSTANCE.wrap( value, options );
			return dateValue == null ? null : new MyDate( dateValue );
		}

		@Override
		public JdbcType getRecommendedJdbcType(final JdbcTypeIndicators context) {
			return context.getJdbcType( SqlTypes.DATE );
		}
	}
}
