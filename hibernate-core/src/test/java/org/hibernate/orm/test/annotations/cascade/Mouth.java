/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;
import java.util.Collection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.REMOVE;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Mouth {
	@Id
	@GeneratedValue
	public Integer id;
	@Column(name="mouth_size")
	public int size;
	@OneToMany(mappedBy = "mouth", cascade = { REMOVE, DETACH } )
	public Collection<Tooth> teeth;
}
