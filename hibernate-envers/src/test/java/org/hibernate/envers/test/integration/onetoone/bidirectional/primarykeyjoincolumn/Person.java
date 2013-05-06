package org.hibernate.envers.test.integration.onetoone.bidirectional.primarykeyjoincolumn;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.io.Serializable;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class Person implements Serializable {
	@Id
	@Column(name = "PERSON_ID")
	@GeneratedValue
	private Long personId;

	private String name;

	@OneToOne(mappedBy = "owner")
	private Account account;

	public Person() {
	}

	public Person(String name) {
		this.name = name;
	}

	public Person(Long personId, String name) {
		this.personId = personId;
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof Person) ) {
			return false;
		}

		Person person = (Person) o;

		if ( personId != null ? !personId.equals( person.personId ) : person.personId != null ) {
			return false;
		}
		if ( name != null ? !name.equals( person.name ) : person.name != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = personId != null ? personId.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "Person(personId = " + personId + ", name = " + name + ")";
	}

	public Long getPersonId() {
		return personId;
	}

	public void setPersonId(Long personId) {
		this.personId = personId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}
}
