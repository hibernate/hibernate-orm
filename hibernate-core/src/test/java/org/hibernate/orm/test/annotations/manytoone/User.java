/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoone;
import java.util.Date;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "CTVUSERS")
@IdClass(UserPK.class)
@SequenceGenerator(name = "UserSeq", sequenceName = "SQ_USER")
public class User {
	@Id
	@Column(name = "CTVUSERS_KEY")
	private Long userKey;

	@Id
	@Column(name = "CTVUSERS_START_DATE")
	private Date startDate;

	@Id
	@Column(name = "CTVUSERS_END_DATE")
	private Date endDate;

	@Column(name = "CTVUSERS_CREATE_USERS_KEY")
	private Long createdBy;

	@Column(name = "CTVUSERS_CREATE_DATE")
	private Date createdOn;

	@Column(name = "CTVUSERS_ID")
	private String userId;

	@Column(name = "CTVUSERS_PREFX_KEY")
	private Integer prefix;

	@Column(name = "CTVUSERS_FIRST_NAME")
	private String firstName;

	@Column(name = "CTVUSERS_LAST_NAME1")
	private String lastName1;

	@Column(name = "CTVUSERS_LAST_NAME2")
	private String lastName2;

	@Column(name = "CTVUSERS_MIDDLE_NAME1")
	private String middleName1;

	@Column(name = "CTVUSERS_MIDDLE_NAME2")
	private String middleName2;

	@Column(name = "CTVUSERS_SUFFX_KEY")
	private Integer suffix;

	@Column(name = "CTVUSERS_BIRTH_DATE")
	private Date birthDate;

	@Column(name = "CTVUSERS_BIRTH_STATE_KEY")
	private Integer birthState;

	@Column(name = "CTVUSERS_BIRTH_CNTRY_KEY")
	private Integer birthCountry;

	@Column(name = "CTVUSERS_USERNAME")
	private String username;

	@Column(name = "CTVUSERS_PASSWORD")
	private String password;

	@Column(name = "CTVUSERS_LOTYP_KEY")
	private Integer userType;

	@Column(name = "CTVUSERS_PRIVL_KEY")
	private Integer privilege;

	@Column(name = "CTVUSERS_STATE_KEY")
	private Integer state;

	@Column(name = "CTVUSERS_CNTRY_KEY")
	private Integer country;

	@Column(name = "CTVUSERS_PREFERRED_NAME")
	private String preferredName;

	@Column(name = "CTVUSERS_BIRTH_PLACE")
	private String birthPlace;

	@OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "user")
	private Set<DistrictUser> districtUsers;

	@Column(name = "CTVUSERS_SCHOL_KEY")
	private Long school;

	@Column(name = "CTVUSERS_CLSTR_KEY")
	private Long cluster;

	@Column(name = "CTVUSERS_LDTMM_KEY")
	private Long ldtmm;

	@Column(name = "CTVUSERS_LDTMD_KEY")
	private Long ldtmd;

	@Column(name = "CTVUSERS_PMTMP_KEY")
	private Long pmtmp;

}
