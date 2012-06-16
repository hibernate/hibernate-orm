//$Id$
package org.hibernate.test.annotations.idmanytoone;
import java.io.Serializable;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
