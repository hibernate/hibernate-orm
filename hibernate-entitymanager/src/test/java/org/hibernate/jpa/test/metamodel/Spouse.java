/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.test.metamodel;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
@Entity
@Table(name = "SPOUSE_TABLE")
public class Spouse implements java.io.Serializable {
	private String id;
	private String first;
	private String maiden;
	private String last;
	private String sNumber;
	private Info info;
	private Customer customer;

	public Spouse() {
	}

	public Spouse(
			String v1, String v2, String v3, String v4,
			String v5, Info v6) {
		id = v1;
		first = v2;
		maiden = v3;
		last = v4;
		sNumber = v5;
		info = v6;
	}


	public Spouse(
			String v1, String v2, String v3, String v4,
			String v5, Info v6, Customer v7) {
		id = v1;
		first = v2;
		maiden = v3;
		last = v4;
		sNumber = v5;
		info = v6;
		customer = v7;
	}

	@Id
	@Column(name = "ID")
	public String getId() {
		return id;
	}

	public void setId(String v) {
		id = v;
	}

	@Column(name = "FIRSTNAME")
	public String getFirstName() {
		return first;
	}

	public void setFirstName(String v) {
		first = v;
	}

	@Column(name = "MAIDENNAME")
	public String getMaidenName() {
		return maiden;
	}

	public void setMaidenName(String v) {
		maiden = v;
	}

	@Column(name = "LASTNAME")
	public String getLastName() {
		return last;
	}

	public void setLastName(String v) {
		last = v;
	}

	@Column(name = "SOCSECNUM")
	public String getSocialSecurityNumber() {
		return sNumber;
	}

	public void setSocialSecurityNumber(String v) {
		sNumber = v;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK_FOR_INFO_TABLE")
	public Info getInfo() {
		return info;
	}

	public void setInfo(Info v) {
		info = v;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "FK7_FOR_CUSTOMER_TABLE")
	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer v) {
		customer = v;
	}

}
