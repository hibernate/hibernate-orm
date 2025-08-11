/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.Tuple;
import org.hibernate.query.Query;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Date;
import java.sql.SQLException;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.Hibernate.getLobHelper;
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
					person.setId( 1 );
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
					final Query<Tuple> query = session.createQuery(
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

}
