/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cfg.persister;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Persister;
import org.hibernate.persister.entity.SingleTableEntityPersister;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
@Entity
@Persister( impl = SingleTableEntityPersister.class)
public class Window {
	@Id
	public Long getId() { return id; }
	public void setId(Long id) {  this.id = id; }
	private Long id;

}
