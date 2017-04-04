/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $
package org.hibernate.test.ops;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Bernard
 */
public class Competition {
	private Integer id;

	private List competitors = new ArrayList();


	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public List getCompetitors() {
		return competitors;
	}

	public void setCompetitors(List competitors) {
		this.competitors = competitors;
	}
}
