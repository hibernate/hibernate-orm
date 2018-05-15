/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.entity;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * @author Emmanuel Bernard
 */
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY)
@Entity
@org.hibernate.annotations.Entity(mutable = false)
public class ZipCode {
	@Id
	public String code;
}
