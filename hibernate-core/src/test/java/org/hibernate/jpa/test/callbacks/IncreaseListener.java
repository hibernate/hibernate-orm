/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.PreUpdate;

/**
 * @author Emmanuel Bernard
 */
public class IncreaseListener {
	@PreUpdate
	public void increate(CommunicationSystem object) {
		object.communication++;
		object.isFirst = false;
		object.isLast = false;
	}
}
