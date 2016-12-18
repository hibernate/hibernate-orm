/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class CTEValues {

	private String idCteSelect;
	private List<Object[]> selectResult = new LinkedList<Object[]>();

	public CTEValues() {
	}

	public CTEValues(String idCteSelect, List<Object[]> selectResult) {
		this.idCteSelect = idCteSelect;
		this.selectResult = selectResult;
	}

	public String getIdCteSelect() {
		return idCteSelect;
	}

	public void setIdCteSelect(String idCteSelect) {
		this.idCteSelect = idCteSelect;
	}

	public List<Object[]> getSelectResult() {
		return selectResult;
	}

	public void setSelectResult(List<Object[]> selectResult) {
		this.selectResult = selectResult;
	}

}
