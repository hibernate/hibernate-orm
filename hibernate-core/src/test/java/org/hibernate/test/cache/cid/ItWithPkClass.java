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
import javax.persistence.IdClass;

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
