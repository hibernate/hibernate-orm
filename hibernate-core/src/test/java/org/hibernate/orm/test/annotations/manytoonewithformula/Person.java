/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.orm.test.annotations.manytoonewithformula;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Sharath Reddy
 */
@Entity
public class Person implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private int id;
	private int companyId;
	private String defaultFlag;
	
	@Id @GeneratedValue
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	@Column(name="company_id")
	public int getCompanyId() {
		return companyId;
	}
	public void setCompanyId(int companyId) {
		this.companyId = companyId;
	}
	@Column(name="is_default")
	public String getDefaultFlag() {
		return defaultFlag;
	}

	public void setDefaultFlag(String defaultFlag) {
		this.defaultFlag = defaultFlag;
	}
}

