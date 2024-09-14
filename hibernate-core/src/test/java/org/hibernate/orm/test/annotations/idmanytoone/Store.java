/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.idmanytoone;
import java.io.Serializable;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
