/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.cid;

import java.io.Serializable;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;

/**
 * @author Steve Ebersole
 */
@Entity
@Cacheable(true)
@IdClass( ItWithPkClass.Pk.class )
public class ItWithPkClass {
	@Id
	public Integer key1;
	@Id
	public Integer key2;
	public String name;

	public static class Pk implements Serializable {
		public Integer key1;
		public Integer key2;
	}
}
