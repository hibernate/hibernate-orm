/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.*;

import java.sql.Date;
import java.sql.SQLException;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.Hibernate.getLobHelper;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(annotatedClasses = Person.class)
@RequiresDialect(AltibaseDialect.class)
@SessionFactory
public class AltibaseFunctionsTest {

	@BeforeAll
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Person person = new Person();
					person.setName( "test.1" );
					ZonedDateTime zonedDateTime = ZonedDateTime.of(
							1990, Month.APRIL.getValue(), 15,
							0, 0, 0, 0,
							ZoneId.of( "UTC" )
					);
					person.setBirthDate( new java.sql.Date( zonedDateTime.toInstant().toEpochMilli() ) ) ;
					person.setWeightInKilograms( 66.0 );
					person.setHeightInMeters( 167.1 );
					person.setIsMarried( false );
					byte[] arry = new byte[ 15 ];
					for (int i = 0; i < arry.length; i++)
					{
						arry[i] = (byte)i;
					}
					person.setBinaryData( getLobHelper().createBlob(arry) );
					person.setComments( getLobHelper().createClob("blahblah") );
					session.persist( person );
				}
		);
	}

	@AfterAll
	public static void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete Person" ).executeUpdate();
				}
		);
	}

	@Test
	public void testSimpleFunction(SessionFactoryScope scope) throws Throwable {
		scope.inTransaction(
				session -> {
					final Person result = session
							.createQuery( "from Person", Person.class )
							.uniqueResult();

					assertNotNull( result );
					assertThat( result.getId(), is( 1 ) );
					assertThat( result.getName(), is( "test.1" ) );
					Date birthDate = result.getBirthDate();
					assertThat( birthDate.toString(), is( "1990-04-15" ) );
					assertThat( result.getWeightInKilograms(), is( 66.0 ) );
					assertThat( result.getHeightInMeters(), is( 167.1 ) );
					assertThat( result.getIsMarried(), is( false ) );
					try {
						assertThat( result.getBinaryData().length(), is( 15L ));
						assertThat( result.getComments().length(), is( 8L ));
					} catch (SQLException e) {
						throw new RuntimeException(e);
					}
				}
		);
	}

	@Test
	public void testLocateFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Person result = session
							.createQuery( "select p from Person p where locate('.', p.name) > 0", Person.class )
							.uniqueResult();
					assertNotNull( result );
					assertThat( result.getName(), is( "test.1" ) );
				}
		);
	}

	@Test
	public void testSubstringFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Person result = session
							.createQuery( "select p from Person p where substring(p.name, 1, 4) = 'test'", Person.class )
							.uniqueResult();
					assertNotNull( result );
					assertThat( result.getName(), is( "test.1" ) );
				}
		);
	}

	@Test
	public void testBitLengthFunction(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final TypedQuery<Tuple> query = session.createQuery(
							"select bit_length(p.comments) from Person p",
							Tuple.class
					);
					final List<Tuple> results = query.getResultList();
					assertThat( results.size(), is( 1 ) );
					final Tuple testEntity = results.get( 0 );
					assertThat( testEntity.get( 0, Integer.class ), is( 64 ) );
				}
		);
	}

	@Test
	public void testTrigonometricFunctionParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final double angle = 1.0d;
					final double ratio = 0.5d;
					final TypedQuery<Tuple> query = session.createQuery(
							"select sin(:angle), cos(:angle), tan(:angle), asin(:ratio), acos(:ratio), atan(:angle), " +
									"atan2(:angle, :ratio), radians(:degreesValue), degrees(:radiansValue) " +
									"from Person p where p.id = 1",
							Tuple.class
					);
					query.setParameter( "angle", angle );
					query.setParameter( "ratio", ratio );
					query.setParameter( "degreesValue", 180.0d );
					query.setParameter( "radiansValue", Math.PI );

					final Tuple result = query.getSingleResult();
					assertEquals( Math.sin( angle ), result.get( 0, Double.class ), 1e-9 );
					assertEquals( Math.cos( angle ), result.get( 1, Double.class ), 1e-9 );
					assertEquals( Math.tan( angle ), result.get( 2, Double.class ), 1e-9 );
					assertEquals( Math.asin( ratio ), result.get( 3, Double.class ), 1e-9 );
					assertEquals( Math.acos( ratio ), result.get( 4, Double.class ), 1e-9 );
					assertEquals( Math.atan( angle ), result.get( 5, Double.class ), 1e-9 );
					assertEquals( Math.atan2( angle, ratio ), result.get( 6, Double.class ), 1e-9 );
					assertEquals( Math.PI, result.get( 7, Double.class ), 1e-9 );
					assertEquals( 180.0d, result.get( 8, Double.class ), 1e-9 );
				}
		);
	}

	@Test
	public void testCoalesceFunctionParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final TypedQuery<Tuple> query = session.createQuery(
							"select coalesce(:value, p.name), coalesce(nullif(p.name, p.name), :value) " +
									"from Person p where p.id = 1",
							Tuple.class
					);
					query.setParameter( "value", "param" );

					final Tuple result = query.getSingleResult();
					assertThat( result.get( 0, String.class ), is( "param" ) );
					assertThat( result.get( 1, String.class ), is( "param" ) );
				}
		);
	}

	@Test
	public void testConcatFunctionParameters(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String result = session.createQuery(
									"select concat(:prefix, p.name, :suffix) from Person p where p.id = 1",
									String.class
							)
							.setParameter( "prefix", "pre-" )
							.setParameter( "suffix", "-post" )
							.getSingleResult();

					assertThat( result, is( "pre-test.1-post" ) );
				}
		);
	}

}
