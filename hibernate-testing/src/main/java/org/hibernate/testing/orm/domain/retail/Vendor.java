/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.domain.retail;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SecondaryTable;

/**
 * @author Steve Ebersole
 */
@Entity
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "vendor_type" )
@SecondaryTable(name = "vendor_supp")
public class Vendor {
	private Integer id;
	private String name;
	private String billingEntity;
	private String supplementalDetail;

	public Vendor() {
	}

	public Vendor(Integer id, String name, String billingEntity) {
		this.id = id;
		this.name = name;
		this.billingEntity = billingEntity;
	}

	public Vendor(Integer id, String name, String billingEntity, String supplementalDetail) {
		this.id = id;
		this.name = name;
		this.billingEntity = billingEntity;
		this.supplementalDetail = supplementalDetail;
	}

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBillingEntity() {
		return billingEntity;
	}

	public void setBillingEntity(String billingEntity) {
		this.billingEntity = billingEntity;
	}

	@Column(table = "vendor_supp")
	public String getSupplementalDetail() {
		return supplementalDetail;
	}

	public void setSupplementalDetail(String supplementalDetail) {
		this.supplementalDetail = supplementalDetail;
	}
}
