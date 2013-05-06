package org.hibernate.envers.test.integration.inheritance.single.discriminatorformula;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
public class ClassTypeEntity {
	public static final String PARENT_TYPE = "Parent";
	public static final String CHILD_TYPE = "Child";

	@Id
	@GeneratedValue
	private Long id;

	private String type;

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ClassTypeEntity) ) {
			return false;
		}

		ClassTypeEntity that = (ClassTypeEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( type != null ? !type.equals( that.type ) : that.type != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (id != null ? id.hashCode() : 0);
		result = 31 * result + (type != null ? type.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "ClassTypeEntity(id = " + id + ", type = " + type + ")";
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
