/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-17739")
@Jpa(annotatedClasses = ListOfStringTest.Unbroken.class)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsStandardArrays.class)
public class ListOfStringTest {
	@Test void test(EntityManagerFactoryScope scope) {
		scope.inTransaction(entityManager -> {
			entityManager.persist( new Unbroken( List.of("hello", "world") ) );
		});
		scope.inTransaction(entityManager -> {
			assertEquals( List.of("hello", "world"),
					entityManager.find(Unbroken.class, 0).stringList );
		});

	}
	@Entity
	static class Unbroken {
		@Id long id;
		List<String> stringList; // this should be OK

		Unbroken(List<String> stringList) {
			this.stringList = stringList;
		}
		Unbroken() {
		}
	}
}
