/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cfg.persister;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author <a href="mailto:emmanuel@hibernate.org">Emmanuel Bernard</a>
 */
@Entity
public class Tree {
    @Id
	public Long getId() { return id; }
	public void setId(Long id) {  this.id = id; }
	private Long id;
}
