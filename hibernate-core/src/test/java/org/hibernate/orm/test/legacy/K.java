/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.legacy;
import java.util.Set;

/**
 * @author Gavin King
 */
public class K {
	private Long id;
	private Set is;
	void setIs(Set is) {
		this.is = is;
	}
	Set getIs() {
		return is;
	}
}
