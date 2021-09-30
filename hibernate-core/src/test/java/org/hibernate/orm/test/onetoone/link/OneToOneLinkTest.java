/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.onetoone.link;

import java.util.Date;

import org.hibernate.Hibernate;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.type.DateType;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = {
				"org/hibernate/orm/test/onetoone/link/Person.hbm.xml"
		}
)
@SessionFactory
public class OneToOneLinkTest {

	@Test
	@SkipForDialect(dialectClass = OracleDialect.class, reason = "oracle12c returns time in getDate.  For now, skip.")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
	public void testOneToOneViaAssociationTable(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					Person p = new Person();
					p.setName( "Gavin King" );
					p.setDob( new Date() );
					Employee e = new Employee();
					p.setEmployee( e );
					e.setPerson( p );
					session.persist( p );
				}
		);

		scope.inTransaction(
				session -> {
					Employee e = (Employee) session.createQuery( "from Employee e where e.person.name like 'Gavin%'" )
							.uniqueResult();
					assertEquals( e.getPerson().getName(), "Gavin King" );
					assertFalse( Hibernate.isInitialized( e.getPerson() ) );
					assertNull( e.getPerson().getCustomer() );
					session.clear();

					e = (Employee) session.createQuery( "from Employee e where e.person.dob = :date" )
							.setParameter( "date", new Date(), DateType.INSTANCE )
							.uniqueResult();
					assertEquals( e.getPerson().getName(), "Gavin King" );
					assertFalse( Hibernate.isInitialized( e.getPerson() ) );
					assertNull( e.getPerson().getCustomer() );
					session.clear();

				}
		);

		scope.inTransaction(
				session -> {
					Employee e = (Employee) session.createQuery(
							"from Employee e join fetch e.person p left join fetch p.customer" )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( e.getPerson() ) );
					assertNull( e.getPerson().getCustomer() );
					Customer c = new Customer();
					e.getPerson().setCustomer( c );
					c.setPerson( e.getPerson() );
				}
		);

		scope.inTransaction(
				session -> {
					Employee e = (Employee) session.createQuery(
							"from Employee e join fetch e.person p left join fetch p.customer" )
							.uniqueResult();
					assertTrue( Hibernate.isInitialized( e.getPerson() ) );
					assertTrue( Hibernate.isInitialized( e.getPerson().getCustomer() ) );
					assertNotNull( e.getPerson().getCustomer() );
					session.delete( e );
				}
		);
	}

}
