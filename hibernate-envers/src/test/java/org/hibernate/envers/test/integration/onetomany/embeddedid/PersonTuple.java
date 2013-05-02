package org.hibernate.envers.test.integration.onetomany.embeddedid;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import java.io.Serializable;

import org.hibernate.envers.Audited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class PersonTuple implements Serializable {
	@Embeddable
	public static class PersonTupleId implements Serializable {
		@Column(nullable = false)
		private long personAId;

		@Column(nullable = false)
		private long personBId;

		@Column(nullable = false)
		private String constantId;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !(o instanceof PersonTupleId) ) {
				return false;
			}

			PersonTupleId that = (PersonTupleId) o;

			if ( personAId != that.personAId ) {
				return false;
			}
			if ( personBId != that.personBId ) {
				return false;
			}
			if ( constantId != null ? !constantId.equals( that.constantId ) : that.constantId != null ) {
				return false;
			}

			return true;
		}

		@Override
		public int hashCode() {
			int result = (int) (personAId ^ (personAId >>> 32));
			result = 31 * result + (int) (personBId ^ (personBId >>> 32));
			result = 31 * result + (constantId != null ? constantId.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "PersonTupleId(personAId = " + personAId + ", personBId = " + personBId + ", constantId = " + constantId + ")";
		}

		public long getPersonAId() {
			return personAId;
		}

		public void setPersonAId(long personAId) {
			this.personAId = personAId;
		}

		public long getPersonBId() {
			return personBId;
		}

		public void setPersonBId(long personBId) {
			this.personBId = personBId;
		}

		public String getConstantId() {
			return constantId;
		}

		public void setConstantId(String constantId) {
			this.constantId = constantId;
		}
	}

	@EmbeddedId
	private PersonTupleId personTupleId = new PersonTupleId();

	@MapsId("personAId")
	@ManyToOne(optional = false)
	@JoinColumn(insertable = false, updatable = false, nullable = false)
	private Person personA;

	@MapsId("personBId")
	@ManyToOne(optional = false)
	@JoinColumn(insertable = false, updatable = false, nullable = false)
	private Person personB;

	@MapsId("constantId")
	@ManyToOne(optional = false)
	@JoinColumn(insertable = false, updatable = false, nullable = false)
	private Constant constant;

	@Column(nullable = false)
	private boolean helloWorld = false;

	public PersonTuple() {
	}

	public PersonTuple(boolean helloWorld, Person personA, Person personB, Constant constant) {
		this.helloWorld = helloWorld;
		this.personA = personA;
		this.personB = personB;
		this.constant = constant;

		this.personTupleId.personAId = personA.getId();
		this.personTupleId.personBId = personB.getId();
		this.personTupleId.constantId = constant.getId();

		personA.getPersonATuples().add( this );
		personB.getPersonBTuples().add( this );
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof PersonTuple) ) {
			return false;
		}

		PersonTuple that = (PersonTuple) o;

		return personTupleId.equals( that.personTupleId );
	}

	@Override
	public int hashCode() {
		return personTupleId.hashCode();
	}

	@Override
	public String toString() {
		return "PersonTuple(id = " + personTupleId + ", helloWorld = " + helloWorld + ")";
	}

	public PersonTupleId getPersonTupleId() {
		return personTupleId;
	}

	public void setPersonTupleId(PersonTupleId personTupleId) {
		this.personTupleId = personTupleId;
	}

	public Person getPersonA() {
		return personA;
	}

	public void setPersonA(Person personA) {
		this.personA = personA;
	}

	public Person getPersonB() {
		return personB;
	}

	public void setPersonB(Person personB) {
		this.personB = personB;
	}

	public Constant getConstant() {
		return constant;
	}

	public void setConstant(Constant constant) {
		this.constant = constant;
	}

	public boolean isHelloWorld() {
		return helloWorld;
	}

	public void setHelloWorld(boolean helloWorld) {
		this.helloWorld = helloWorld;
	}
}