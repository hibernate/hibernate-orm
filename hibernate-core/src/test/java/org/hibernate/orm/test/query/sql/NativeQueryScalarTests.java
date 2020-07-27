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

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.internal.BasicValuedSingularAttributeMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.convert.spi.JpaAttributeConverter;
import org.hibernate.orm.test.metamodel.mapping.SmokeTests;
import org.hibernate.query.sql.spi.NativeQueryImplementor;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
public class NativeQueryScalarTests {
	public static final String STRING_VALUE = "a string value";
	public static final String URL_STRING = "http://hibernate.org";

	@Test
	public void fullyImplicitTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select theString, theInteger, id from EntityOfBasics";
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
	public void explicitOrderTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select theString, theInteger, id from EntityOfBasics";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					// notice the reverse order from the select clause
					query.addScalar( "id" );
					query.addScalar( "theInteger" );
					query.addScalar( "theString" );

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
					assertThat( result, instanceOf( String.class ) );

					assertThat( result, is( "O" ) );
				}
		);


		// Converter instance
		scope.inTransaction(
				session -> {
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					query.addScalar(
							"converted_gender",
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

	@BeforeAll
	public void verifyModel(SessionFactoryScope scope) {
		final EntityMappingType entityDescriptor = scope.getSessionFactory()
				.getRuntimeMetamodels()
				.getEntityMappingType( EntityOfBasics.class );

		final ModelPart part = entityDescriptor.findSubPart( "convertedGender", null );
		assertThat( part, instanceOf( BasicValuedSingularAttributeMapping.class ) );
		final BasicValuedSingularAttributeMapping attrMapping = (BasicValuedSingularAttributeMapping) part;

		assertThat( attrMapping.getJavaTypeDescriptor().getJavaType(), equalTo( EntityOfBasics.Gender.class ) );

		final BasicValueConverter valueConverter = attrMapping.getValueConverter();
		assertThat( valueConverter, instanceOf( JpaAttributeConverter.class ) );
		assertThat( valueConverter.getDomainJavaDescriptor(), is( attrMapping.getJavaTypeDescriptor() ) );
		assertThat( valueConverter.getRelationalJavaDescriptor().getJavaType(), equalTo( Character.class ) );

		assertThat( attrMapping.getJdbcMapping().getSqlTypeDescriptor().getJdbcTypeCode(), is( Types.CHAR ) );
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
				}
		);

		scope.inTransaction(
				session -> {
					final EntityOfBasics entity = session.get( EntityOfBasics.class, 1 );
					assertThat( entity, notNullValue() );
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete EntityOfBasics" ).executeUpdate()
		);
	}
}
