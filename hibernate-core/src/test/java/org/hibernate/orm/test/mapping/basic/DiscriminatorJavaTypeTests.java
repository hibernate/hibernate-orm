/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic;

import org.hibernate.annotations.JavaType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JiraKey("HHH-16490")
@DomainModel(annotatedClasses = {
		DiscriminatorJavaTypeTests.BaseClass.class,
		DiscriminatorJavaTypeTests.Subclass.class
})
@SessionFactory
public class DiscriminatorJavaTypeTests {

	@Test
	public void testResolution(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Subclass subclass = new Subclass();
					session.persist( subclass );
					session.flush();
					session.clear();
					final BaseClass baseClass = session.find( BaseClass.class, subclass.getId() );
					assertNotNull( baseClass.getCode() );
					assertEquals( "test", baseClass.getCode().getCode() );
				}
		);
	}

	@Entity(name = "BaseClass")
	@DiscriminatorColumn(name = "code")
	public static abstract class BaseClass {
		@Id
		@GeneratedValue
		private Integer id;

		@JavaType(CodeJavaType.class)
		@Column(name = "code", nullable = false, insertable = false, updatable = false)
		private Code code;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Code getCode() {
			return code;
		}

		public void setCode(Code code) {
			this.code = code;
		}
	}

	@Entity(name = "Subclass")
	@DiscriminatorValue( "test" )
	public static class Subclass extends BaseClass {

	}

	public static class CodeJavaType extends AbstractClassJavaType<Code> {
		public CodeJavaType() {
			super( Code.class );
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return indicators.getJdbcType( SqlTypes.VARCHAR );
		}

		@Override
		public <X> X unwrap(Code value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( type == Code.class ) {
				return (X) value;
			}
			if ( type == String.class ) {
				return (X) value.getCode();
			}
			throw unknownUnwrap(type);
		}

		@Override
		public <X> Code wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}
			if ( value instanceof Code ) {
				return (Code) value;
			}
			if ( value instanceof String ) {
				return new Code( (String) value );
			}
			throw unknownWrap(value.getClass());
		}
	}

	public static class Code {
		private String code;

		public Code(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}
}
