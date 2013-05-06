package org.hibernate.envers.test.integration.customtype;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.io.Serializable;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

/**
 * Entity encapsulating {@link Object} property which concrete type may change during subsequent updates.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Entity
@Audited
public class ObjectUserTypeEntity implements Serializable {
	@Id
	@GeneratedValue
	private int id;

	private String buildInType;

	@Type(type = "org.hibernate.envers.test.integration.customtype.ObjectUserType")
	@Columns(columns = {@Column(name = "OBJ_TYPE"), @Column(name = "OBJ_VALUE")})
	private Object userType;

	public ObjectUserTypeEntity() {
	}

	public ObjectUserTypeEntity(String buildInType, Object userType) {
		this.buildInType = buildInType;
		this.userType = userType;
	}

	public ObjectUserTypeEntity(int id, String buildInType, Object userType) {
		this.id = id;
		this.buildInType = buildInType;
		this.userType = userType;
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ObjectUserTypeEntity) ) {
			return false;
		}

		ObjectUserTypeEntity that = (ObjectUserTypeEntity) o;

		if ( id != that.id ) {
			return false;
		}
		if ( buildInType != null ? !buildInType.equals( that.buildInType ) : that.buildInType != null ) {
			return false;
		}
		if ( userType != null ? !userType.equals( that.userType ) : that.userType != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = id;
		result = 31 * result + (buildInType != null ? buildInType.hashCode() : 0);
		result = 31 * result + (userType != null ? userType.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ObjectUserTypeEntity(id = " + id + ", buildInType = " + buildInType + ", userType = " + userType + ")";
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getBuildInType() {
		return buildInType;
	}

	public void setBuildInType(String buildInType) {
		this.buildInType = buildInType;
	}

	public Object getUserType() {
		return userType;
	}

	public void setUserType(Object userType) {
		this.userType = userType;
	}
}
