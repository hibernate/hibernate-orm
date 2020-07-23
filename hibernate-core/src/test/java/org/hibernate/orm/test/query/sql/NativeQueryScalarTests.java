/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sql;

import java.time.LocalDate;
import java.util.List;
import javax.persistence.Entity;

import org.hibernate.query.sql.spi.NativeQueryImplementor;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.hamcrest.CoreMatchers;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		standardModels = StandardDomainModel.CONTACTS
)
@SessionFactory
public class NativeQueryScalarTests {
	@Test
	public void fullyImplicitTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select gender, first, last, id from contacts";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );
					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Object[].class ) );

					final Object[] values = (Object[]) result;
					assertThat( values.length, is(4 ) );

					assertThat( ( (Number) values[0] ).intValue(), is( Contact.Gender.OTHER.ordinal() ) );
					assertThat( values[1], is( "My First" ) );
					assertThat( values[2], is( "Contact" ) );
					assertThat( values[3], is( 1 ) );
				}
		);
	}
	@Test
	public void explicitOrderTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select gender, first, last, id from contacts";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					// notice the reverse order from the select clause
					query.addScalar( "id" );
					query.addScalar( "last" );
					query.addScalar( "first" );
					query.addScalar( "gender" );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );
					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Object[].class ) );

					final Object[] values = (Object[]) result;
					assertThat( values.length, is(4 ) );

					assertThat( values[0], is( 1 ) );
					assertThat( values[1], is( "Contact" ) );
					assertThat( values[2], is( "My First" ) );
					assertThat( ( (Number) values[3] ).intValue(), is( Contact.Gender.OTHER.ordinal() ) );
				}
		);
	}

	@Test
	@FailureExpected( reason = "Explicit type support not working atm" )
	public void explicitEnumTypeTest(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final String sql = "select gender, first, last, id from contacts";
					final NativeQueryImplementor<?> query = session.createNativeQuery( sql );
					// notice the reverse order from the select clause
					query.addScalar( "id" );
					query.addScalar( "last" );
					query.addScalar( "first" );
					query.addScalar( "gender", scope.getSessionFactory().getTypeConfiguration().getBasicTypeForJavaType( Contact.Gender.class ) );

					final List<?> results = query.list();
					assertThat( results.size(), is( 1 ) );
					final Object result = results.get( 0 );
					assertThat( result, instanceOf( Object[].class ) );

					final Object[] values = (Object[]) result;
					assertThat( values.length, is(4 ) );

					assertThat( values[0], is( 1 ) );
					assertThat( values[1], is( "Contact" ) );
					assertThat( values[2], is( "My First" ) );
					assertThat( values[3], is( Contact.Gender.OTHER ) );
				}
		);
	}

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist(
							new Contact(
									1,
									new Contact.Name( "My First", "Contact"),
									Contact.Gender.OTHER,
									LocalDate.EPOCH
							)
					);
				}
		);
	}

	@AfterEach
	public void cleanUpData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createQuery( "delete Contact" ).executeUpdate()
		);
	}
}
