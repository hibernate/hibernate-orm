/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
@Table(name = "Bs")
public class Customer implements Serializable {
    @Id @GeneratedValue
	public Integer id;

    @OneToMany(mappedBy = "customer")
    public Set<StoreCustomer> stores;

    private static final long serialVersionUID = 3818501706063039923L;
}
