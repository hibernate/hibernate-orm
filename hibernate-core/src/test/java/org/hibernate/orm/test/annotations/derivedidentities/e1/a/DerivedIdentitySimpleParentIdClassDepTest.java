/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.a;

import java.util.List;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Dependent.class,
				Employee.class
		}
)
@SessionFactory
public class DerivedIdentitySimpleParentIdClassDepTest {

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		final MetadataImplementor metadata = scope.getMetadataImplementor();
		assertTrue( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", metadata ) );
		assertTrue( !SchemaUtil.isColumnPresent( "Dependent", "emp", metadata ) );

		Employee e = new Employee( 1L, "Emmanuel", "Manu" );
		Dependent dependent = new Dependent( "Doggy", e );
		scope.inTransaction(
				session -> {
					session.persist( dependent );
					session.persist( e );
				}
		);

		scope.inTransaction(
				session -> {
					DependentId dId = new DependentId( dependent.getName(), dependent.getEmp().empId );
					Dependent d = session.get( Dependent.class, dId );
					assertEquals( e.empId, d.getEmp().empId );
					assertEquals( e.empName, d.getEmp().empName );
					assertEquals( e.nickname, d.getEmp().nickname );
					session.remove( d );
					session.remove( d.getEmp() );
				}
		);
	}

	@Test
	public void testQueryNewEntityInPC(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Employee e = new Employee( 1L, "Paula", "P" );
					Dependent d = new Dependent( "LittleP", e );
					d.setEmp( e );
					session.persist( d );
					session.persist( e );

					// find the entity added above
					Query query = session.createQuery(
							"Select d from Dependent d where d.name='LittleP' and d.emp.empName='Paula'" );
					List depList = query.list();
					assertEquals( 1, depList.size() );
					Object newDependent = depList.get( 0 );
					assertSame( d, newDependent );
					session.remove( d );
					session.remove( d.getEmp() );
				}
		);
	}
}
