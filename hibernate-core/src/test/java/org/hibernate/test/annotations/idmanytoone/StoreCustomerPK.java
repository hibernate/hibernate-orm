/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.idmanytoone;
import java.io.Serializable;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
public class StoreCustomerPK implements Serializable {
	StoreCustomerPK() {}
	@Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "idA")
    public Store store;

    @Id
	@ManyToOne(optional = false)
    @JoinColumn(name = "idB")
    public Customer customer;


    public StoreCustomerPK(Store store, Customer customer) {
	this.store = store;
	this.customer = customer;
    }


    private static final long serialVersionUID = -1102111921432271459L;
}
