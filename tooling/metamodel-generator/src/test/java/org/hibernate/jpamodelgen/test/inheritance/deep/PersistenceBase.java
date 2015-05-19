/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.inheritance.deep;

import java.util.Date;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;

/**
 * A mapped super class that does not define an id attribute.
 *
 * @author Igor Vaynberg
 */
@MappedSuperclass
public abstract class PersistenceBase {
	Date created;

	@PrePersist
	void prePersist() {
		created = new Date();
	}
}
