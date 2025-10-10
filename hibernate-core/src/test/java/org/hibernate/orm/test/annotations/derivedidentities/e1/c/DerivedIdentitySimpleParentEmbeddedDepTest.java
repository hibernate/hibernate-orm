/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.c;

import org.hibernate.Session;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 */
@SessionFactory
@DomainModel(
		annotatedClasses = {
				Dependent.class,
				Employee.class
		}
)
public class DerivedIdentitySimpleParentEmbeddedDepTest {

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();

		assertThat( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", metadata ) ).isTrue();
		assertThat( !SchemaUtil.isColumnPresent( "Dependent", "empPK", metadata ) ).isTrue();
		Employee e = new Employee();
		e.empId = 1;
		e.empName = "Emmanuel";
		scope.inTransaction(
				session -> {
					session.persist( e );
					Dependent d = new Dependent();
					d.emp = e;
					d.name = "Doggy";
					session.persist( d );
					session.flush();
					session.clear();
					d = getDerivedClassById( e, d.name, session );
					assertThat( d.emp.empId ).isEqualTo( e.empId );
				}
		);
	}

	private Dependent getDerivedClassById(Employee e, String name, Session s) {
		final String qry = "from Dependent d where d.name = :name and d.emp.empId = :empId";

		return s.createQuery( qry, Dependent.class )
				.setParameter( "empId", e.empId )
				.setParameter( "name", name )
				.uniqueResult();
	}
}
