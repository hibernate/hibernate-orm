/*
  * Hibernate, Relational Persistence for Idiomatic Java
  *
  * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-
  * party contributors as indicated by the @author tags or express
  * copyright attribution statements applied by the authors.
  * All third-party contributions are distributed under license by
  * Red Hat, Inc.
  *
  * This copyrighted material is made available to anyone wishing to
  * use, modify, copy, or redistribute it subject to the terms and
  * conditions of the GNU Lesser General Public License, as published
  * by the Free Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  * Lesser General Public License for more details.
  *
  * You should have received a copy of the GNU Lesser General Public
  * License along with this distribution; if not, write to:
  *
  * Free Software Foundation, Inc.
  * 51 Franklin Street, Fifth Floor
  * Boston, MA  02110-1301  USA
  */

package org.hibernate.test.annotations.manytoonewithformula;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JoinFormula;

/**
 * @author Sharath Reddy
 *
 */
@Entity
@Table(name="product")
public class ProductSqlServer implements Serializable
{

	private static final long serialVersionUID = 6956478993159505828L;

	@Id
    public Integer id;

    @Column(name="product_idnf", length=18, nullable=false, unique=true,
        columnDefinition="char(18)")
    public String productIdnf;

    @Column(name="description", nullable=false)
    public String description;

    @ManyToOne
	@JoinFormula(value="SUBSTRING(product_idnf, 1, 3)",
				 referencedColumnName="product_idnf")
	@Fetch(FetchMode.JOIN)
    private ProductSqlServer productFamily;

    public ProductSqlServer getProductFamily()
    {
        return productFamily;
    }

}
