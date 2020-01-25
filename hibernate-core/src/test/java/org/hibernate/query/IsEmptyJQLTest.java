package org.hibernate.query;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Nathan Xu
 */
@TestForIssue(jiraKey = "HHH-6686")
public class IsEmptyJQLTest extends BaseEntityManagerFunctionalTestCase {

	private Long personWithoutNicknameId = 1L;
	private Long personaWithSingleNicknameId = 2L;
	private Long personWithMultipleNicknamesId = 3L;
	
	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}
	
	@Test
	public void testJQLContainingEmpty() {
		List<Person> personWithNicknames = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"select p from Person p where p.nicknames is not empty", Person.class )
					.getResultList();
		});
		
		assertEquals( new HashSet<>( Arrays.asList(personaWithSingleNicknameId, personWithMultipleNicknamesId)), 
				personWithNicknames.stream().map( Person::getId ).collect( Collectors.toSet() ));

		List<Person> personWithOutNickname = doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
					"select p from Person p where p.nicknames is empty", Person.class )
					.getResultList();
		});

		assertEquals( Collections.singleton(personWithoutNicknameId), 
				personWithOutNickname.stream().map( Person::getId ).collect( Collectors.toSet() ));
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person personaWithoutNickname = new Person();
			personaWithoutNickname.setId(personWithoutNicknameId);
			
			Person personaWithSingleNickname = new Person();
			personaWithSingleNickname.getNicknames().add( "nickname" );
			personaWithSingleNickname.setId(personaWithSingleNicknameId);
			
			Person personWithMultipleNicknames = new Person();
			personWithMultipleNicknames.getNicknames().addAll( Arrays.asList( "nickName1", "nickName2" ) );
			personWithMultipleNicknames.setId(personWithMultipleNicknamesId);
			
			entityManager.persist( personaWithoutNickname );
			entityManager.persist( personaWithSingleNickname );
			entityManager.persist( personWithMultipleNicknames );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;
		
		@ElementCollection
		private List<String> nicknames = new ArrayList<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
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


