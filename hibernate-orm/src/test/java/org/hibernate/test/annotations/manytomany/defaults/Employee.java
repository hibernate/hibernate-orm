/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.manytomany.defaults;
import java.io.Serializable;
import java.util.Collection;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToMany;

import org.hibernate.annotations.Cascade;

/**
 * Employee in an Employer-Employee relationship
 *
 * @author Emmanuel Bernard
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@SuppressWarnings("serial")
public class Employee implements Serializable {
	private Integer id;
	private String name;
	ContactInfo contactInfo;

	// ContactInfo is for ManyToMany testing
	@Embedded
	public ContactInfo getContactInfo() {
		return contactInfo;
	}

	public void setContactInfo(ContactInfo contactInfo) {
		this.contactInfo = contactInfo;
	}

	@Column(name="fld_name")
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer integer) {
		id = integer;
	}
}
