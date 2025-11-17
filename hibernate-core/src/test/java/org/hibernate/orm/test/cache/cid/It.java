/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.cid;

import java.io.Serializable;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Steve Ebersole
 */
@Entity
@Cacheable(true)
public class It implements Serializable {
	@Id
	public Integer key1;
	@Id
	public Integer key2;
	public String name;
}
