/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
@Table(name = "Bs")
public class Customer implements Serializable {
	@Id @GeneratedValue
	public Integer id;

	@OneToMany(mappedBy = "customer")
	public Set<StoreCustomer> stores;

	private static final long serialVersionUID = 3818501706063039923L;
}
