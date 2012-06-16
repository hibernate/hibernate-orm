//$Id$
package org.hibernate.test.annotations.idmanytoone;
import java.io.Serializable;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "`As`")
public class Store implements Serializable {
    @Id @GeneratedValue
	public Integer id;

    @OneToMany(mappedBy = "store")
    public Set<StoreCustomer> customers;


    private static final long serialVersionUID = 1748046699322502790L;
}

