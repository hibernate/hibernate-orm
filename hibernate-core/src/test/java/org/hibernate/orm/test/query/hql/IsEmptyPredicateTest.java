/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-6686")
@ServiceRegistry
@DomainModel( annotatedClasses = IsEmptyPredicateTest.Person.class )
@SessionFactory
public class IsEmptyPredicateTest {

	private final Integer personWithoutNicknameId = 1;
	private final Integer personaWithSingleNicknameId = 2;
	private final Integer personWithMultipleNicknamesId = 3;

	@Test
	public void testEmptinessPredicates(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<Integer> ids = session.createQuery( "select p.id from Person p where p.nicknames is not empty", Integer.class ).list();
			assertThat( ids ).contains( personaWithSingleNicknameId, personWithMultipleNicknamesId );
			assertThat( ids ).doesNotContain( personWithoutNicknameId );
		} );

		scope.inTransaction( (session) -> {
			final List<Integer> ids = session.createQuery( "select p.id from Person p where p.nicknames is empty", Integer.class ).list();
			assertThat( ids ).contains( personWithoutNicknameId );
			assertThat( ids ).doesNotContain( personaWithSingleNicknameId, personWithMultipleNicknamesId );
		} );
	}

	@Test
	public void testEmptinessPredicatesWithJoin(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<Integer> ids = session.createQuery( "select p.id from Person p left join p.nicknames n where p.nicknames is not empty", Integer.class ).list();
			assertThat( ids ).contains( personaWithSingleNicknameId, personWithMultipleNicknamesId );
			assertThat( ids ).doesNotContain( personWithoutNicknameId );
		} );
	}

	@BeforeEach
	protected void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			Person personaWithoutNickname = new Person();
			personaWithoutNickname.setId(personWithoutNicknameId);

			Person personaWithSingleNickname = new Person();
			personaWithSingleNickname.getNicknames().add( "nickname" );
			personaWithSingleNickname.setId(personaWithSingleNicknameId);

			Person personWithMultipleNicknames = new Person();
			personWithMultipleNicknames.getNicknames().addAll( Arrays.asList( "nickName1", "nickName2" ) );
			personWithMultipleNicknames.setId(personWithMultipleNicknamesId);

			session.persist( personaWithoutNickname );
			session.persist( personaWithSingleNickname );
			session.persist( personWithMultipleNicknames );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Integer id;

		@ElementCollection
		private List<String> nicknames = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<String> getNicknames() {
			return nicknames;
		}

		public void setNicknames(List<String> nicknames) {
			this.nicknames = nicknames;
		}
	}
}
