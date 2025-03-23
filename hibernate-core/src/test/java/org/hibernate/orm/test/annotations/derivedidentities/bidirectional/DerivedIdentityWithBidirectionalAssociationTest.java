/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;

import org.hibernate.Session;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hardy Ferentschik
 */
@DomainModel(
		annotatedClasses = {
				Dependent.class,
				Employee.class
		}
)
@SessionFactory
public class DerivedIdentityWithBidirectionalAssociationTest {

	@Test
	public void testBidirectionalAssociation(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "empPK", metadata ) );

		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";

		scope.inTransaction(
				session -> {
					session.persist( e );
					Dependent d = new Dependent();
					d.emp = e;
					session.persist( d );
					session.flush();
					session.clear();
					d = getDerivedClassById( e, session, Dependent.class );
					assertEquals( e.empId, d.emp.empId );
				}
		);
	}

	private <T> T getDerivedClassById(Employee e, Session s, Class<T> clazz) {
		return clazz.cast( s.createQuery( "from " + clazz.getName() + " d where d.emp.empId = :empId" )
								.setParameter( "empId", e.empId ).uniqueResult() );
	}
}
