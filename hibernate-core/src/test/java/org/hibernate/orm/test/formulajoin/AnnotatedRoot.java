/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.formulajoin;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Steve Ebersole
 */
@Entity
public class AnnotatedRoot {
	@Id
	private Integer id;
	private String name;
	@ManyToOne(fetch= FetchType.EAGER, optional=false)
	@JoinColumnOrFormula(formula=@JoinFormula(value="my_domain_key'", referencedColumnName="detail_domain"))
	@JoinColumnOrFormula(column=@JoinColumn(name="detail", referencedColumnName="id"))
	@Fetch(FetchMode.JOIN)
	@NotNull
	private AnnotatedDetail detail;
}
