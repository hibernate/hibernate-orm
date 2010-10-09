//$Id: $
package org.hibernate.test.nonflushedchanges;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Emmanuel Bernard, Gail Badner (adapted this from "ops" tests version)
 */
public class Competition implements Serializable {
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
