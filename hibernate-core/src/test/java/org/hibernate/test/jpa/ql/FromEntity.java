package org.hibernate.test.jpa.ql;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Janario Oliveira
 */
@Entity
@Table(name = "from_entity")
public class FromEntity {

	@Id
	@GeneratedValue
	Integer id;
	@Column(nullable = false)
	String name;
	@Column(nullable = false)
	String lastName;

	public FromEntity() {
	}

	public FromEntity(String name, String lastName) {
		this.name = name;
		this.lastName = lastName;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 53 * hash + ( this.name != null ? this.name.hashCode() : 0 );
		hash = 53 * hash + ( this.lastName != null ? this.lastName.hashCode() : 0 );
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj == null ) {
			return false;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}
		final FromEntity other = (FromEntity) obj;
		if ( ( this.name == null ) ? ( other.name != null ) : !this.name.equals( other.name ) ) {
			return false;
		}
		if ( ( this.lastName == null ) ? ( other.lastName != null ) : !this.lastName.equals( other.lastName ) ) {
			return false;
		}
		return true;
	}
}
