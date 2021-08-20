/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.discriminator;

import javax.persistence.Tuple;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/discriminator/Person.hbm.xml"
)
@SessionFactory
public class DiscriminatorQueryUsageTests {
	private Long steveId;

	@Test
	public void testUsageAsSelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Tuple resultTuple = session
					.createQuery( "select p.id as id, type(p) as type from Person p", Tuple.class )
					.uniqueResult();

			Assertions.assertThat( resultTuple.get( "id" ) ).isEqualTo( steveId );
			Assertions.assertThat( resultTuple.get( "type" ) ).isEqualTo( Employee.class );
		} );
	}

	@Test
	public void testUsageAsPredicate(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Long id = session.createQuery( "select p.id from Person p where type(p) = Employee", Long.class ).uniqueResult();
			Assertions.assertThat( id ).isEqualTo( steveId );
		} );
	}

	@Test
	public void testUsageAsPredicateOfUnderlyingType(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Long id = session.createQuery( "select p.id from Person p where type(p) = 'P'", Long.class ).uniqueResult();
			Assertions.assertThat( id ).isEqualTo( steveId );
		} );
	}

	@Test
	public void testUsageAsPredicateWithParam(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Long id = session.createQuery( "select p.id from Person p where type(p) = :type", Long.class )
					.setParameter( "type", Employee.class )
					.uniqueResult();
			Assertions.assertThat( id ).isEqualTo( steveId );
		} );
	}

	@Test
	public void testUsageAsPredicateWithParamOfUnderlyingType(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Long id = session.createQuery( "select p.id from Person p where type(p) = :type", Long.class )
					.setParameter( "type", "P" )
					.uniqueResult();
			Assertions.assertThat( id ).isEqualTo( steveId );
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		final Employee created = scope.fromTransaction( (session) -> {
			final Employee e = new Employee();
			e.setName( "Steve" );
			e.setSex( 'M' );
			e.setTitle( "Gangster of love" );

			session.save( e );

			return e;
		} );

		steveId = created.getId();
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete from Person" ).executeUpdate();
		} );
	}
}
