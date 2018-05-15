/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cache.cid;

import java.io.Serializable;
import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Id;

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
