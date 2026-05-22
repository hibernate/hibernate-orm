/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.LIBRARY,
		annotatedClasses = Jpa4ParameterBindingTests.CodedEntity.class
)
@SessionFactory
public class Jpa4ParameterBindingTests {

	@BeforeEach
	void prepareData(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createMutationQuery( "delete from CodedEntity" ).executeUpdate();
			session.persist( new CodedEntity( 1, new Code( "A" ) ) );
			session.persist( new CodedEntity( 2, new Code( "B" ) ) );
		} );
	}

	@Test
	void testHql(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createQuery( "from Book where id = :id", Book.class )
					.setConvertedParameter( "id", "123", StringToIntConverter.class )
					.list();

			session.createQuery( "from Book where id = ?1", Book.class )
					.setConvertedParameter( 1, "123", StringToIntConverter.class )
					.list();

			session.createQuery( "from Book where id = :id", Book.class )
					.setParameter( "id", 1, Integer.class )
					.list();

			session.createQuery( "from Book where id = ?1", Book.class )
					.setParameter( 1, 1, Integer.class )
					.list();
		} );
	}

	@Test
	void testHqlConvertedParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final var namedResults =
					session.createQuery( "from CodedEntity where code = :code", CodedEntity.class )
							.setConvertedParameter( "code", new Code( "A" ), CodeConverter.class )
							.list();
			assertEquals( 1, namedResults.size() );
			assertEquals( new Code( "A" ), namedResults.get( 0 ).getCode() );

			final var positionalResults =
					session.createQuery( "from CodedEntity where code = ?1", CodedEntity.class )
							.setConvertedParameter( 1, new Code( "B" ), CodeConverter.class )
							.list();
			assertEquals( 1, positionalResults.size() );
			assertEquals( new Code( "B" ), positionalResults.get( 0 ).getCode() );
		} );
	}

	@Test
	void testMutationQueryConvertedParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final int updated =
					session.createMutationQuery( "update CodedEntity set code = :updated where code = :original" )
							.setConvertedParameter( "updated", new Code( "B" ), CodeConverter.class )
							.setConvertedParameter( "original", new Code( "A" ), CodeConverter.class )
							.executeUpdate();
			assertEquals( 1, updated );

			final var results =
					session.createQuery( "from CodedEntity where id = 1", CodedEntity.class )
							.list();
			assertEquals( new Code( "B" ), results.get( 0 ).getCode() );
		} );
	}

	@Test
	void testNativeQuery(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			session.createNativeQuery( "select * from books where id = :id", Book.class )
					.setConvertedParameter( "id", "123", StringToIntConverter.class )
					.list();

			session.createNativeQuery( "select * from books where id = ?1", Book.class )
					.setConvertedParameter( 1, "123", StringToIntConverter.class )
					.list();

			session.createNativeQuery( "select * from books where id = :id", Book.class )
					.setParameter( "id", 1, Integer.class )
					.list();

			session.createNativeQuery( "select * from books where id = ?1", Book.class )
					.setParameter( 1, 1, Integer.class )
					.list();
		} );

	}

	@Test
	void testNativeQueryConvertedParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final var results =
					session.createNativeQuery( "select * from coded_entities where code = :code", CodedEntity.class )
							.setConvertedParameter( "code", new Code( "A" ), CodeConverter.class )
							.list();
			assertEquals( 1, results.size() );
			assertEquals( new Code( "A" ), results.get( 0 ).getCode() );
		} );
	}

	@Test
	void testCriteriaConvertedParameter(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final CriteriaQuery<CodedEntity> criteria = builder.createQuery( CodedEntity.class );
			final Root<CodedEntity> root = criteria.from( CodedEntity.class );
			final ParameterExpression<Code> parameter = builder.convertedParameter( CodeConverter.class );

			criteria.select( root ).where( builder.equal( root.get( "code" ), parameter ) );

			final var results =
					session.createQuery( criteria )
							.setParameter( parameter, new Code( "A" ) )
							.list();
			assertEquals( 1, results.size() );
			assertEquals( new Code( "A" ), results.get( 0 ).getCode() );
		} );
	}

	@Test
	void testProcedureCallConvertedParameterRegistration(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (session) -> {
			final var positionalCall = session.createStoredProcedureCall( "unused" );
			final var positionalParameter =
					positionalCall.registerConvertedParameter( 1, CodeConverter.class, ParameterMode.IN );
			positionalCall.setParameter( positionalParameter, new Code( "A" ) );
			assertEquals( new Code( "A" ), positionalCall.getParameterValue( 1 ) );

			final var namedCall = session.createStoredProcedureCall( "unused" );
			final var namedParameter =
					namedCall.registerConvertedParameter( "code", CodeConverter.class, ParameterMode.IN );
			namedCall.setParameter( namedParameter, new Code( "B" ) );
			assertEquals( new Code( "B" ), namedCall.getParameterValue( "code" ) );
		} );
	}

	public static class StringToIntConverter implements AttributeConverter<String,Integer> {
		@Override
		public Integer convertToDatabaseColumn(String domainValue) {
			return domainValue == null ? null : Integer.parseInt( domainValue );
		}

		@Override
		public String convertToEntityAttribute(Integer relationalValue) {
			return relationalValue == null ? null : Integer.toString( relationalValue );
		}
	}

	public record Code(String value) {
	}

	public static class CodeConverter implements AttributeConverter<Code, String> {
		@Override
		public String convertToDatabaseColumn(Code domainValue) {
			return domainValue == null ? null : domainValue.value();
		}

		@Override
		public Code convertToEntityAttribute(String relationalValue) {
			return relationalValue == null ? null : new Code( relationalValue );
		}
	}

	@Entity(name = "CodedEntity")
	@Table(name = "coded_entities")
	public static class CodedEntity {
		@Id
		private Integer id;

		@Column(name = "code")
		@Convert(converter = CodeConverter.class)
		private Code code;

		protected CodedEntity() {
		}

		public CodedEntity(Integer id, Code code) {
			this.id = id;
			this.code = code;
		}

		public Code getCode() {
			return code;
		}
	}
}
