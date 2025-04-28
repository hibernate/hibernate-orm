/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.type.contributor.usertype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.Session;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.Type;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractClassJavaType;
import org.hibernate.type.descriptor.java.ImmutableMutabilityPlan;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.usertype.UserType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(
		annotatedClasses = BasicValuedMappingUserTypePropertyTest.Book.class,
		typeContributors = BasicValuedMappingUserTypePropertyTest.MyDateTypeContributor.class
)
@SessionFactory
@JiraKey( "HHH-19392" )
public class BasicValuedMappingUserTypePropertyTest {

	@BeforeAll
	void afterEntityManagerFactoryBuilt(SessionFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.persist(
						new Book()
								.setIsbn( "978-9730228236" )
								.setLocalDate( MyDate.of( 1985, 10, 21 ) )
				) );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Book book = entityManager.unwrap( Session.class )
					.bySimpleNaturalId( Book.class )
					.load( "978-9730228236" );

			book.setLocalDate( MyDate.of( 2025, 4, 27 ) );
		} );

		scope.inSession( entityManager -> {
			MyDate date = entityManager
					.createNativeQuery(
							"SELECT " +
							"  localDate AS localDate " +
							"FROM book " +
							"WHERE " +
							"  isbn = :isbn", MyDate.class )
					.setParameter( "isbn", "978-9730228236" )
					.getSingleResult();

			assertEquals( MyDate.of( 2025, 4, 27 ).getDate(), date.getDate() );
		} );
	}

	@Entity(name = "Book")
	@Table(name = "book")
	public static class Book {

		@Id
		@GeneratedValue
		private Long id;

		@NaturalId
		private String isbn;

		@Type(MyDateType.class)
		@Column(columnDefinition = "VARCHAR(16)")
		private MyDate localDate;

		public String getIsbn() {
			return isbn;
		}

		public Book setIsbn(String isbn) {
			this.isbn = isbn;
			return this;
		}

		public MyDate getLocalDate() {
			return localDate;
		}

		public Book setLocalDate(MyDate localDate) {
			this.localDate = localDate;
			return this;
		}
	}

	public static class MyDate {

		private static final Pattern P = Pattern.compile( "\\[(.*)]" );

		private LocalDate date;

		public static MyDate of(int y, int m, int d) {
			return new MyDate( LocalDate.of( y, m, d ) );
		}

		public MyDate() {
		}

		public MyDate(LocalDate date) {
			this.date = date;
		}

		public static MyDate parse(CharSequence string) {
			if ( string == null ) {
				return null;
			}
			final var matcher = P.matcher( string );
			if ( !matcher.matches() ) {
				throw new IllegalArgumentException();
			}
			return new MyDate( LocalDate.parse( matcher.group( 1 ) ) );
		}

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

		@Override
		public String toString() {
			return "[%s]".formatted( date == null ? "" : DateTimeFormatter.ISO_LOCAL_DATE.format( date ) );
		}
	}

	public static class MyDateType implements UserType<MyDate>, BasicValuedMapping {

		private final MyDateJdbcTypeDescriptor jdbcTypeDescriptor;
		private final MyDateJavaTypeDescriptor javaTypeDescriptor;
		private final JdbcMapping jdbcMapping;

		public static final MyDateType INSTANCE = new MyDateType();

		MyDateType() {
			this.jdbcTypeDescriptor = MyDateJdbcTypeDescriptor.INSTANCE;
			this.javaTypeDescriptor = MyDateJavaTypeDescriptor.INSTANCE;
			this.jdbcMapping = new BasicTypeImpl<>( javaTypeDescriptor, jdbcTypeDescriptor );

			javaTypeDescriptor.setJdbcType( jdbcTypeDescriptor );
		}

		@Override
		public MyDate nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
			return jdbcTypeDescriptor.getExtractor( javaTypeDescriptor ).extract( rs, position, options );
		}

		@Override
		public void nullSafeSet(PreparedStatement st, MyDate value, int position, WrapperOptions options)
				throws SQLException {
			jdbcTypeDescriptor.getBinder( javaTypeDescriptor ).bind( st, value, position, options );
		}

		@Override
		public MyDate deepCopy(MyDate value) {
			return javaTypeDescriptor.getMutabilityPlan().deepCopy( value );
		}

		@Override
		public boolean isMutable() {
			return javaTypeDescriptor.getMutabilityPlan().isMutable();
		}

		@Override
		public int getSqlType() {
			return jdbcTypeDescriptor.getJdbcTypeCode();
		}

		@Override
		public Class<MyDate> returnedClass() {
			return javaTypeDescriptor.getJavaTypeClass();
		}

		@Override
		public JdbcMapping getJdbcMapping() {
			return jdbcMapping;
		}

		@Override
		public MappingType getMappedType() {
			return jdbcMapping;
		}

		@Override
		public <X, Y> int forEachDisassembledJdbcValue(Object value, int offset, X x, Y y, JdbcValuesBiConsumer<X, Y> valuesConsumer, SharedSessionContractImplementor session) {
			valuesConsumer.consume( offset, x, y, value, jdbcMapping );
			return getJdbcTypeCount();
		}

		@Override
		public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
			action.accept( offset, jdbcMapping );
			return getJdbcTypeCount();
		}
	}

	public static class MyDateJdbcTypeDescriptor implements JdbcType {

		public static final MyDateJdbcTypeDescriptor INSTANCE = new MyDateJdbcTypeDescriptor();

		@Override
		public int getJdbcTypeCode() {
			return Types.VARCHAR;
		}

		@Override
		public <X> ValueBinder<X> getBinder(final JavaType<X> javaType) {
			return new BasicBinder<>( javaType, this ) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
						throws SQLException {
					st.setString( index, javaType.unwrap( value, String.class, options ) );
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
						throws SQLException {
					st.setString( name, javaType.unwrap( value, String.class, options ) );
				}
			};
		}

		protected Object extractJson(ResultSet rs, int paramIndex) throws SQLException {
			return rs.getString( paramIndex );
		}

		protected Object extractJson(CallableStatement statement, int index) throws SQLException {
			return statement.getString( index );
		}

		protected Object extractJson(CallableStatement statement, String name) throws SQLException {
			return statement.getString( name );
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(final JavaType<X> javaType) {
			return new BasicExtractor<>( javaType, this ) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					return javaType.wrap( extractJson( rs, paramIndex ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, int index, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( extractJson( statement, index ), options );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
						throws SQLException {
					return javaType.wrap( extractJson( statement, name ), options );
				}
			};
		}
	}

	public static class MyDateJavaTypeDescriptor extends AbstractClassJavaType<MyDate> {

		private JdbcType jdbcType;

		public static final MyDateJavaTypeDescriptor INSTANCE = new MyDateJavaTypeDescriptor();

		private MyDateJavaTypeDescriptor() {
			super( MyDate.class, ImmutableMutabilityPlan.instance() );
		}

		@Override
		public MyDate fromString(CharSequence string) {
			return MyDate.parse( string );
		}

		@Override
		public <X> X unwrap(MyDate value, Class<X> type, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}

			if ( CharSequence.class.isAssignableFrom( type ) ) {
				//noinspection unchecked
				return (X) value.toString();
			}
			else if ( LocalDate.class.isAssignableFrom( type ) ) {
				//noinspection unchecked
				return (X) value.getDate();
			}
			else if ( MyDate.class.isAssignableFrom( type ) ) {
				//noinspection unchecked
				return (X) value;
			}

			throw unknownUnwrap( type );
		}

		@Override
		public <X> MyDate wrap(X value, WrapperOptions options) {
			if ( value == null ) {
				return null;
			}

			if ( value instanceof MyDate localDate ) {
				return localDate;
			}
			else if ( value instanceof LocalDate localDate ) {
				return new MyDate( localDate );
			}

			else {
				return fromString( (CharSequence) value );
			}
		}

		@Override
		public JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
			return jdbcType;
		}

		@Override
		public Class<MyDate> getJavaTypeClass() {
			return MyDate.class;
		}

		public void setJdbcType(JdbcType jdbcType) {
			this.jdbcType = jdbcType;
		}
	}

	static class MyDateTypeContributor implements TypeContributor {
		@Override
		public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
			typeContributions.contributeType( MyDateType.INSTANCE );
		}
	}
}
