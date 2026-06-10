/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;

import java.util.Collection;
import java.util.HashSet;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.id.IncrementGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * @author Gavin King
 */
@Entity
@Table(name = "ORGANIZATION")
@SQLInsert(sql = "INSERT INTO ORGANIZATION (NAME, ORGID) VALUES ( UPPER(? || ''), ? )")
@SQLUpdate(sql = "UPDATE ORGANIZATION SET NAME=UPPER(? || '') WHERE ORGID=?")
@SQLDelete(sql = "DELETE FROM ORGANIZATION WHERE ORGID=?")
@SQLSelect(sql = """
		SELECT ORGID, NAME
		FROM ORGANIZATION
		WHERE ORGID=?
		""")
@NamedNativeQuery(
		name = "allOrganizationsWithEmployees",
		query = """
				SELECT DISTINCT org.NAME, org.ORGID
				FROM ORGANIZATION org
				INNER JOIN EMPLOYMENT e ON e.EMPLOYER = org.ORGID
				""",
		resultClass = Organization.class
)
public class Organization {
	@Id
	@GenericGenerator(type = IncrementGenerator.class)
	@Column(name = "ORGID")
	private long id;

	@Column(name = "NAME", nullable = false)
	private String name;

	@OneToMany(mappedBy = "employer")
	@SQLSelect(sql = """
			SELECT EMPID, EMPLOYEE, EMPLOYER, STARTDATE, ENDDATE, REGIONCODE, VALUE, CURRENCY
			FROM EMPLOYMENT
			WHERE EMPLOYER = ?
			ORDER BY STARTDATE ASC, EMPLOYEE ASC
			""")
	private Collection<Employment> employments;

	public Organization(String name) {
		this.name = name;
		employments = new HashSet<>();
	}

	public Organization() {}

	/**
	 * @return Returns the employments.
	 */
	public Collection<Employment> getEmployments() {
		return employments;
	}
	/**
	 * @param employments The employments to set.
	 */
	public void setEmployments(Collection<Employment> employments) {
		this.employments = employments;
	}
	/**
	 * @return Returns the id.
	 */
	public long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(long id) {
		this.id = id;
	}
	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

}
