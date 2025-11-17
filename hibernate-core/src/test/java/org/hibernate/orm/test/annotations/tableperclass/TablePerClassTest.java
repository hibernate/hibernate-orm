/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.tableperclass;

import java.util.List;
import jakarta.persistence.PersistenceException;

import org.hibernate.JDBCException;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Robot.class,
				T800.class,
				Machine.class,
				Component.class,
				Product.class
		}
)
@SessionFactory
public class TablePerClassTest {

	@Test
	public void testUnionSubClass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Machine computer = new Machine();
					computer.setWeight( new Double( 4 ) );
					Robot asimov = new Robot();
					asimov.setWeight( new Double( 120 ) );
					asimov.setName( "Asimov" );
					T800 terminator = new T800();
					terminator.setName( "Terminator" );
					terminator.setWeight( new Double( 300 ) );
					terminator.setTargetName( "Sarah Connor" );
					session.persist( computer );
					session.persist( asimov );
					session.persist( terminator );
				}
		);

		scope.inTransaction(
				session -> {
					Query q = session.createQuery( "from Machine m where m.weight >= :weight" );
					q.setParameter( "weight", new Double( 10 ) );
					List result = q.list();
					assertEquals( 2, result.size() );
				}
		);
	}

	@Test
	public void testConstraintsOnSuperclassProperties(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Product product1 = new Product();
					product1.setId( 1l );
					product1.setManufacturerId( 1l );
					product1.setManufacturerPartNumber( "AAFR" );
					session.persist( product1 );
					session.flush();
					Product product2 = new Product();
					product2.setId( 2l );
					product2.setManufacturerId( 1l );
					product2.setManufacturerPartNumber( "AAFR" );
					session.persist( product2 );
					try {
						session.flush();
						fail( "Database Exception not handled" );
					}
					catch (PersistenceException e) {
						assertTyping( JDBCException.class, e );
						//success
					}
				}
		);
	}

	public static <T> T assertTyping(Class<T> expectedType, Object value) {
		if ( !expectedType.isInstance( value ) ) {
			fail(
					String.format(
							"Expecting value of type [%s], but found [%s]",
							expectedType.getName(),
							value == null ? "<null>" : value
					)
			);
		}
		return (T) value;
	}
}
