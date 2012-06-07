//$Id$
package org.hibernate.test.annotations.idmanytoone;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "ABs")
@IdClass( StoreCustomerPK.class)
public class StoreCustomer implements Serializable {
	StoreCustomer() {}
	@Id
    @ManyToOne(optional = false)
    @JoinColumn(name = "idA")
    public Store store;

    @Id
	@ManyToOne(optional = false)
    @JoinColumn(name = "idB")
    public Customer customer;


    public StoreCustomer(Store store, Customer customer) {
	this.store = store;
	this.customer = customer;
    }


    private static final long serialVersionUID = -8295955012787627232L;
}
