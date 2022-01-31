/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.compliance;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Jpa
public class StoreProcedureTest {

	@Test
	public void createNotExistingStoredProcedureQuery(EntityManagerFactoryScope scope) {

		scope.inTransaction(
				entityManager -> {
					Assertions.assertThrows(
							IllegalArgumentException.class,
							() -> {
								entityManager.createStoredProcedureQuery(
												"NOT_EXISTING_NAME",
												"NOT_EXISTING_RESULT_MAPPING"
										)
										.execute();
							}
					);
				} );

	}

}
