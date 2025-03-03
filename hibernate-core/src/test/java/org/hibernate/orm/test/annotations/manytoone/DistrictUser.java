/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;
import java.util.Date;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "CLRUS2DI")
public class DistrictUser {
	@Id
	@GeneratedValue
	@Column(name = "CLRUS2DI_KEY")
	private Long id;

	@Column(name = "CLRUS2DI_CREATE_USERS_KEY")
	private Long createdBy;

	@Column(name = "CLRUS2DI_CREATE_DATE")
	private Date createdOn;

	//@ManyToOne(cascade = CascadeType.ALL)
	//@JoinColumn(name = "CLRUS2DI_DISTR_KEY")
	//private District district;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "CLRUS2DI_USERS_KEY", referencedColumnName = "CTVUSERS_KEY")
	@JoinColumn(name = "CLRUS2DI_BEGIN_DATE", referencedColumnName = "CTVUSERS_START_DATE")
	@JoinColumn(name = "CLRUS2DI_END_DATE", referencedColumnName = "CTVUSERS_END_DATE")
	private User user;

	@Column(name = "CLRUS2DI_LDTMD_KEY")
	private Long ldtmd;

	@Column(name = "CLRUS2DI_PMTMP_KEY")
	private Long pmtmp;
}
