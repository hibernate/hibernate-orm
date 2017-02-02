/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import java.util.List;

/**
 * @author Gail Badner
 */
public class Bug extends Reportable {
	private List<String> detail;

	public List<String> getDetail() {
		return detail;
	}
	public void setDetail(List<String> detail) {
		this.detail = detail;
	}
}
