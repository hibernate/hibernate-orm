/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.array;

import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/array/A.hbm.xml"
)
@SessionFactory
public class ArrayTest {

	@Test
	@SkipForDialect(dialectClass = AbstractHANADialect.class, reason = " HANA doesn't support tables consisting of only a single auto-generated column")
	public void testArrayJoinFetch(SessionFactoryScope scope) {
		A a = new A();
		scope.inTransaction(
				session -> {
					B b = new B();
					a.setBs( new B[] { b } );
					session.persist( a );
				}
		);

		scope.inTransaction(
				session -> {
					A retrieved = session.get( A.class, a.getId() );
					assertNotNull( retrieved );
					assertNotNull( retrieved.getBs() );
					assertEquals( 1, retrieved.getBs().length );
					assertNotNull( retrieved.getBs()[0] );

					session.delete( retrieved );
					session.delete( retrieved.getBs()[0] );
				}
		);
	}
}
