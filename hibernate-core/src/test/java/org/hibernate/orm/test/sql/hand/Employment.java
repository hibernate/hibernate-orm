/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.hand;
import java.util.Date;

import org.hibernate.annotations.CompositeType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLInsert;
import org.hibernate.annotations.SQLSelect;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.id.IncrementGenerator;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedStoredProcedureQueries;
import jakarta.persistence.NamedStoredProcedureQuery;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureParameter;
import jakarta.persistence.Table;

/**
 * @author Gavin King
 */
@Entity
@Table(name = "EMPLOYMENT")
@SQLInsert(sql = """
		INSERT INTO EMPLOYMENT
			(EMPLOYEE, EMPLOYER, STARTDATE, REGIONCODE, VALUE, CURRENCY, EMPID)
		VALUES (?, ?, TIMESTAMP ('2006-02-28 11:39:00'), UPPER(? || ''), ?, ?, ?)
		""")
@SQLUpdate(sql = "UPDATE EMPLOYMENT SET ENDDATE=?, VALUE=?, CURRENCY=? WHERE EMPID=?")
@SQLDelete(sql = "DELETE FROM EMPLOYMENT WHERE EMPID=?")
@SQLSelect(sql = """
		SELECT EMPLOYEE, EMPLOYER, STARTDATE, ENDDATE, REGIONCODE, EMPID, VALUE, CURRENCY
		FROM EMPLOYMENT
		WHERE EMPID = ?
		""")
@NamedStoredProcedureQueries({
		@NamedStoredProcedureQuery(
				name = "simpleScalar",
				procedureName = "simpleScalar",
				parameters = @StoredProcedureParameter(name = "p_number", mode = ParameterMode.IN, type = Integer.class)
		),
		@NamedStoredProcedureQuery(
				name = "paramhandling",
				procedureName = "paramHandling",
				parameters = {
						@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
						@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class)
				}
		),
		@NamedStoredProcedureQuery(
				name = "paramhandling_mixed",
				procedureName = "paramHandling",
				parameters = {
						@StoredProcedureParameter(mode = ParameterMode.IN, type = Integer.class),
						@StoredProcedureParameter(name = "second", mode = ParameterMode.IN, type = Integer.class)
				}
		),
		@NamedStoredProcedureQuery(
				name = "selectAllEmployments",
				procedureName = "selectAllEmployments",
				resultClasses = Employment.class
		)
})
public class Employment {
	@Id
	@GenericGenerator(type = IncrementGenerator.class)
	@Column(name = "EMPID")
	private long employmentId;

	@ManyToOne
	@JoinColumn(name = "EMPLOYEE", nullable = false, updatable = false)
	private Person employee;

	@ManyToOne
	@JoinColumn(name = "EMPLOYER", nullable = false, updatable = false)
	private Organization employer;

	@Column(name = "STARTDATE", nullable = false, insertable = false, updatable = false)
	private Date startDate;

	@Column(name = "ENDDATE", insertable = false)
	private Date endDate;

	@Column(name = "REGIONCODE", updatable = false)
	private String regionCode;

	@CompositeType(MonetaryAmountUserType.class)
	@AttributeOverrides({
			@AttributeOverride(name = "value", column = @Column(name = "VALUE")),
			@AttributeOverride(name = "currency", column = @Column(name = "CURRENCY"))
	})
	private MonetaryAmount salary;

	public Employment() {}

	public Employment(Person employee, Organization employer, String regionCode) {
		this.employee = employee;
		this.employer = employer;
		this.startDate = new Date();
		this.regionCode = regionCode;
		employer.getEmployments().add(this);
	}
	/**
	 * @return Returns the employee.
	 */
	public Person getEmployee() {
		return employee;
	}
	/**
	 * @param employee The employee to set.
	 */
	public void setEmployee(Person employee) {
		this.employee = employee;
	}
	/**
	 * @return Returns the employer.
	 */
	public Organization getEmployer() {
		return employer;
	}
	/**
	 * @param employer The employer to set.
	 */
	public void setEmployer(Organization employer) {
		this.employer = employer;
	}
	/**
	 * @return Returns the endDate.
	 */
	public Date getEndDate() {
		return endDate;
	}
	/**
	 * @param endDate The endDate to set.
	 */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	/**
	 * @return Returns the id.
	 */
	public long getEmploymentId() {
		return employmentId;
	}
	/**
	 * @param id The id to set.
	 */
	public void setEmploymentId(long id) {
		this.employmentId = id;
	}
	/**
	 * @return Returns the startDate.
	 */
	public Date getStartDate() {
		return startDate;
	}
	/**
	 * @param startDate The startDate to set.
	 */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	/**
	 * @return Returns the regionCode.
	 */
	public String getRegionCode() {
		return regionCode;
	}
	/**
	 * @param regionCode The regionCode to set.
	 */
	public void setRegionCode(String regionCode) {
		this.regionCode = regionCode;
	}

	public MonetaryAmount getSalary() {
		return salary;
	}

	public void setSalary(MonetaryAmount salary) {
		this.salary = salary;
	}
}
