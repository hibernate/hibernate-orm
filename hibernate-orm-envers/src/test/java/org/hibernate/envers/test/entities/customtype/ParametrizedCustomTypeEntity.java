/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.entities.customtype;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.envers.Audited;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Entity
@TypeDef(name = "param", typeClass = ParametrizedTestUserType.class,
		 parameters = {@Parameter(name = "param1", value = "x"), @Parameter(name = "param2", value = "y")})
@Table(name = "ParamCustType")
public class ParametrizedCustomTypeEntity {
	@Id
	@GeneratedValue
	private Integer id;

	@Audited
	@Type(type = "param")
	private String str;

	public ParametrizedCustomTypeEntity() {
	}

	public ParametrizedCustomTypeEntity(Integer id, String str) {
		this.id = id;
		this.str = str;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !(o instanceof ParametrizedCustomTypeEntity) ) {
			return false;
		}

		ParametrizedCustomTypeEntity that = (ParametrizedCustomTypeEntity) o;

		if ( id != null ? !id.equals( that.id ) : that.id != null ) {
			return false;
		}
		if ( str != null ? !str.equals( that.str ) : that.str != null ) {
			return false;
		}

		return true;
	}

	public int hashCode() {
		int result;
		result = (id != null ? id.hashCode() : 0);
		result = 31 * result + (str != null ? str.hashCode() : 0);
		return result;
	}

	public String toString() {
		return "PCTE(id = " + id + ", str = " + str + ")";
	}
}