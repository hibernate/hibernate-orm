/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.type;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Dvd.class
		}
)
@SessionFactory
public class TypeTest {

	@Test
	public void testIdWithMulticolumns(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Dvd lesOiseaux = new Dvd();
					lesOiseaux.setTitle( "Les oiseaux" );
					session.persist( lesOiseaux );
					session.flush();
					assertThat( lesOiseaux.getId() ).isNotNull();
				}
		);
	}
}
