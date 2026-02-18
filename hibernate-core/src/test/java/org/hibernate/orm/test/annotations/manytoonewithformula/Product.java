/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.manytoonewithformula;
import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Sharath Reddy
 *
 */
@Entity
@Table(name="product")
public class Product implements Serializable
{

	private static final long serialVersionUID = 6956478993159505828L;

	@Id
	public Integer id;

	@Column(name="product_idnf", length=18, nullable=false, unique=true)
	public String productIdnf;

	@Column(name="description", nullable=false)
	public String description;

	@ManyToOne
	@JoinFormula(value="{fn substring(product_idnf, 1, 3)}",
				referencedColumnName="product_idnf")
	@Fetch(FetchMode.JOIN)
	private Product productFamily;

	public Product getProductFamily()
	{
		return productFamily;
	}

}
