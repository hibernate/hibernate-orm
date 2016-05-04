/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.jpa.test.callbacks;
import javax.persistence.EntityListeners;
import javax.persistence.MappedSuperclass;

/**
 * @author Emmanuel Bernard
 */
@MappedSuperclass
@EntityListeners(IncreaseListener.class)
public class CommunicationSystem {
	public int communication = 0;
	public boolean isFirst = true;
	public boolean isLast;

	public void init() {
		communication = 0;
		isFirst = true;
		isLast = false;
	}
}
