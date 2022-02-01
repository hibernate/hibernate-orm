/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = GetJavaTypeTest.Person.class
)
public class GetJavaTypeTest {

	@Test
	public void getJavaType(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					Metamodel metaModel = entityManager.getMetamodel();
					if ( metaModel != null ) {
						ManagedType<Person> mTypeOrder = metaModel.managedType( Person.class );
						assertNotNull( mTypeOrder );
						Attribute<Person, ?> attrib = mTypeOrder.getDeclaredAttribute( "age" );
						assertNotNull( attrib );
						Class pAttribJavaType = attrib.getJavaType();
						assertEquals( "int", pAttribJavaType.getName() );
					}
				}
		);
	}

	@Entity(name = "Person")
	@Table(name = "PERSON_TABLE")
	public static class Person {
		@Id
		private int id;

		private String name;

		private int age;
	}
}
