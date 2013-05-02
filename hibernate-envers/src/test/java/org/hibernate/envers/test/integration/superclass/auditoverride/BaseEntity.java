package org.hibernate.envers.test.integration.superclass.auditoverride;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@MappedSuperclass
public class BaseEntity implements Serializable {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	private String str1;

	@NotAudited
	private Integer number1;

	public BaseEntity() {
	}

	public BaseEntity(String str1, Integer number1, Integer id) {
		this.id = id;
		this.str1 = str1;
		this.number1 = number1;
	}

	public BaseEntity(String str1, Integer number1) {
		this.str1 = str1;
		this.number1 = number1;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr1() {
		return str1;
	}

	public void setStr1(String str1) {
		this.str1 = str1;
	}

	public Integer getNumber1() {
		return number1;
	}

	public void setNumber1(Integer number1) {
		this.number1 = number1;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof BaseEntity) ) {
			return false;
		}

		BaseEntity that = (BaseEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( number1 != null ? !number1.equals( that.number1 ) : that.number1 != null ) {
			return false;
		}
		if ( str1 != null ? !str1.equals( that.str1 ) : that.str1 != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str1 != null ? str1.hashCode() : 0);
		result = 31 * result + (number1 != null ? number1.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "BaseEntity(id = " + id + ", str1 = " + str1 + ", number1 = " + number1 + ")";
	}
}
