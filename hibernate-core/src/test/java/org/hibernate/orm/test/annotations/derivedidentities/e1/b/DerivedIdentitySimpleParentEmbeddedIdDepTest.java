/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e1.b;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 */
@SessionFactory
@DomainModel(
		annotatedClasses = {
				Dependent.class,
				Employee.class,
				ExclusiveDependent.class
		}
)
public class DerivedIdentitySimpleParentEmbeddedIdDepTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testManyToOne(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();
		assertThat( SchemaUtil.isColumnPresent( "Dependent", "emp_empId", metadata ) ).isTrue();
		assertThat( !SchemaUtil.isColumnPresent( "Dependent", "empPK", metadata ) ).isTrue();

		scope.inTransaction(
				session -> {
					Employee e = new Employee();
					e.empId = 1;
					e.empName = "Emmanuel";
					Dependent d = new Dependent();
					d.emp = e;
					d.id = new DependentId();
					d.id.name = "Doggy";
					session.persist( d );
					session.persist( e );
					session.flush();
					session.clear();
					d = session.find( Dependent.class, d.id );
					assertThat( d.emp.empId ).isEqualTo( d.id.empPK );
				}
		);
	}

	@Test
	public void testOneToOne(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();
		assertThat( SchemaUtil.isColumnPresent( "ExclusiveDependent", "FK", metadata ) ).isTrue();
		assertThat( !SchemaUtil.isColumnPresent( "ExclusiveDependent", "empPK", metadata ) ).isTrue();

		scope.inTransaction(
				session -> {
					Employee e = new Employee();
					e.empId = 1;
					e.empName = "Emmanuel";
					session.persist( e );
					ExclusiveDependent d = new ExclusiveDependent();
					d.emp = e;
					d.id = new DependentId();
					d.id.name = "Doggy";
					//d.id.empPK = e.empId; //FIXME not needed when foreign is enabled
					session.persist( d );
					session.flush();
					session.clear();
					d = session.find( ExclusiveDependent.class, d.id );
					assertThat( d.emp.empId ).isEqualTo( d.id.empPK );
				}
		);
	}

}
