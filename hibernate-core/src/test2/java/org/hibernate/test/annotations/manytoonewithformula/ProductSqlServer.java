/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
