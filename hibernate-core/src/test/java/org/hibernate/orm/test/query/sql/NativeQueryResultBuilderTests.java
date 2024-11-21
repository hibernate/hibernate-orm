/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Types;
import java.time.Instant;
import java.util.List;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.BasicAttributeMapping;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assumptions;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.GAMBIT
)
@SessionFactory
public class NativeQueryResultBuilderTests {
	public static final String STRING_VALUE = "a string value";
	public static final String URL_STRING = "http://hibernate.org";

	@Test
	public void fullyImplicitTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select the_string, the_integer, id from EntityOfBasics";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Object[].class ) );

					final Object[] values = (Object[]) result;
					assertThat( values.length, is(3 ) );

					assertThat( values[ 0 ], is( STRING_VALUE ) );
					assertThat( values[ 1 ], is( 2 ) );
					assertThat( values[ 2 ], is( 1 ) );
				}
		);
	}

	@Test
	public void fullyImplicitTest2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// DB2, Derby, SQL Server and Sybase return an Integer for count by default
					// Oracle returns a NUMERIC(39,0) i.e. a BigDecimal for count by default
					Assumptions.assumeThat( session.getJdbcServices().getDialect() )
							.isNotInstanceOf( DB2Dialect.class )
							.isNotInstanceOf( DerbyDialect.class )
							.isNotInstanceOf( SQLServerDialect.class )
							.isNotInstanceOf( SybaseDialect.class )
							.isNotInstanceOf( OracleDialect.class );
					final String sql = "select count(the_string) from EntityOfBasics";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Long.class ) );

					assertThat( result, is( 1L ) );
				}
		);
	}

	@Test
	public void explicitOrderTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select the_string, the_integer, id from EntityOfBasics";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					// notice the reverse order from the select clause
					query.addScalar( "id" );
					query.addScalar( "the_integer" );
					query.addScalar( "the_string" );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Object[].class ) );

					final Object[] values = (Object[]) result;
					assertThat( values.length, is(3 ) );

					assertThat( values[ 0 ], is( 1 ) );
					assertThat( values[ 1 ], is( 2 ) );
					assertThat( values[ 2 ], is( STRING_VALUE ) );
				}
		);
	}

	@Test
	public void explicitEnumTypeTest(SessionFactoryScope scope) {
		final String sql = "select id, gender, ordinal_gender from EntityOfBasics";

		// first, without explicit typing
		scope.inTransaction(
				session -> {
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					query.addScalar( "id" );
					query.addScalar( "gender" );
					query.addScalar( "ordinal_gender" );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Object[].class ) );

					final Object[] values = (Object[]) result;
					assertThat( values.length, is(3 ) );

					assertThat( values[ 0 ], is( 1 ) );
					assertThat( values[ 1 ], is( "MALE" ) );
					assertThat( values[ 2 ], matchesOrdinal( EntityOfBasics.Gender.FEMALE ) );
				}
		);

		// then using explicit typing
		scope.inTransaction(
				session -> {
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					query.addScalar( "id" );
					query.addScalar( "gender", EntityOfBasics.Gender.class );
					query.addScalar( "ordinal_gender", EntityOfBasics.Gender.class );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Object[].class ) );

					final Object[] values = (Object[]) result;
					assertThat( values.length, is(3 ) );

					assertThat( values[ 0 ], is( 1 ) );
					assertThat( values[ 1 ], is( EntityOfBasics.Gender.MALE ) );
					assertThat( values[ 2 ], is( EntityOfBasics.Gender.FEMALE ) );
				}
		);
	}
	@Test
	public void explicitConversionTest(SessionFactoryScope scope) {
		final String sql = "select converted_gender from EntityOfBasics";

		// Control
		scope.inTransaction(
				session -> {
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Character.class ) );

					assertThat( result, is( 'O' ) );
				}
		);


		// Converter instance
		scope.inTransaction(
				session -> {
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					query.addScalar(
							"converted_gender",
							EntityOfBasics.Gender.class,
							Character.class,
							new EntityOfBasics.GenderConverter()
					);

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( EntityOfBasics.Gender.class ) );

					assertThat( result, is( EntityOfBasics.Gender.OTHER ) );
				}
		);

		// Converter class
		scope.inTransaction(
				session -> {
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					query.addScalar(
							"converted_gender",
							EntityOfBasics.Gender.class,
							Character.class,
							EntityOfBasics.GenderConverter.class
					);

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( EntityOfBasics.Gender.class ) );

					assertThat( result, is( EntityOfBasics.Gender.OTHER ) );
				}
		);
	}

	private Matcher matchesOrdinal(Enum enumValue) {
		return new CustomMatcher<Object>( "Enum ordinal value" ) {
			@Override
			public boolean matches(Object item) {
				return ( (Number) item ).intValue() == enumValue.ordinal();
			}
		};
	}

	@Test
	public void testConvertedAttributeBasedBuilder(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final NativeQueryImplementor qry = session.createNativeQuery(
							"select converted_gender from EntityOfBasics"
					);

					qry.addAttributeResult(
							"converted_gender",
							"EntityOfBasics",
							"convertedGender"
					);

					final List results = qry.list();
					assertThat( results.size(), is( 1 ) );

					final Object result = results.get( 0 );
					assertThat( result, instanceOf( EntityOfBasics.Gender.class ) );

					assertThat( result, is( EntityOfBasics.Gender.OTHER ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-18629")
	public void testNativeQueryWithResultClass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select data, id from BasicEntity";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql, BasicEntity.class );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final BasicEntity result = (BasicEntity) results.get( 0 );

					assertThat( result.getData(), is( STRING_VALUE ) );
					assertThat( result.getId(), is( 1 ) );
				}
		);
	}

	@Test
	@JiraKey("HHH-18629")
	public void testNativeQueryWithResultClassAndPlaceholders(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select {be.*} from BasicEntity be";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql, BasicEntity.class );
					query.addEntity( "be", BasicEntity.class );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );

					final BasicEntity result = (BasicEntity) results.get( 0 );

					assertThat( result.getData(), is( STRING_VALUE ) );
					assertThat( result.getId(), is( 1 ) );
				}
		);
	}

	@BeforeAll
	public void verifyModel(SessionFactoryScope scope) {
		final EntityMappingType entityDescriptor = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( EntityOfBasics.class );
		final JdbcTypeRegistry jdbcTypeRegistry = scope.getSessionFactory()
				.getTypeConfiguration()
				.getJdbcTypeRegistry();
		final ModelPart part = entityDescriptor.findSubPart( "convertedGender", null );
		assertThat( part, instanceOf( BasicAttributeMapping.class ) );
		final BasicAttributeMapping attrMapping = (BasicAttributeMapping) part;

		assertThat( attrMapping.getJavaType().getJavaTypeClass(), equalTo( EntityOfBasics.Gender.class ) );

		final BasicValueConverter valueConverter = attrMapping.getJdbcMapping().getValueConverter();
		assertThat( valueConverter, instanceOf( JpaAttributeConverter.class ) );
		assertThat( valueConverter.getDomainJavaType(), is( attrMapping.getJavaType() ) );
		assertThat( valueConverter.getRelationalJavaType().getJavaTypeClass(), equalTo( Character.class ) );

		assertThat( attrMapping.getJdbcMapping().getJdbcType(), is( jdbcTypeRegistry.getDescriptor( Types.CHAR ) ) );
	}

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) throws MalformedURLException {
		final URL url = new URL( URL_STRING );

		scope.inTransaction(
				session -> {
					final EntityOfBasics entityOfBasics = new EntityOfBasics( 1 );
					entityOfBasics.setTheString( STRING_VALUE );
					entityOfBasics.setTheInteger( 2 );
					entityOfBasics.setGender( EntityOfBasics.Gender.MALE );
					entityOfBasics.setOrdinalGender( EntityOfBasics.Gender.FEMALE );
					entityOfBasics.setConvertedGender( EntityOfBasics.Gender.OTHER );
					entityOfBasics.setTheUrl( url );
					entityOfBasics.setTheInstant( Instant.EPOCH );

					session.persist( entityOfBasics );

					session.persist( new BasicEntity( 1, STRING_VALUE ) );
				}
		);

		scope.inTransaction(
				session -> {
					assertThat( session.get( EntityOfBasics.class, 1 ), notNullValue() );

					assertThat( session.get( BasicEntity.class, 1 ), notNullValue() );
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete EntityOfBasics" ).executeUpdate();
					session.createQuery( "delete BasicEntity" ).executeUpdate();
				}
		);
	}

	public static class DTO {
		private Integer key;
		private String text;

		public DTO(Integer key, String text) {
			this.key = key;
			this.text = text;
		}
	}

	public static class Bean {
		private Integer key;
		private String text;

		public Integer getKey() {
			return key;
		}

		public void setKey(Integer key) {
			this.key = key;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
