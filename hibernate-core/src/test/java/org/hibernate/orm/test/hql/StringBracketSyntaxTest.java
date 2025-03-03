/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(standardModels = StandardDomainModel.GAMBIT)
@SessionFactory
@JiraKey("HHH-18089")
public class StringBracketSyntaxTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					BasicEntity entity = new BasicEntity(1, "Hello World");
					session.persist( entity );
				}
		);
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from BasicEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testCharAtSyntax(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Character firstChar = session.createQuery( "select e.data[1] from BasicEntity e", Character.class )
							.getSingleResult();
					assertThat( firstChar ).isEqualTo( 'H' );
				}
		);
	}

	@Test
	public void testSubstringSyntax(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					String substring = session.createQuery( "select e.data[1:6] from BasicEntity e", String.class )
							.getSingleResult();
					assertThat( substring ).isEqualTo( "Hello " );
				}
		);
	}


}
