/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.test.annotations.entity;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * @author Sharath Reddy
 */
@FilterDef(name="byState", defaultCondition=":state = state",
		parameters=@ParamDef(name="state",type="string"))
@Entity
public class Topic {

	@Id @GeneratedValue
	private int id;
	@OneToMany(mappedBy="topic", cascade=CascadeType.ALL)
	@Filter(name="byState", condition=":state = state")
	private Set<Narrative> narratives = new HashSet<Narrative>();
	
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Set<Narrative> getNarratives() {
		return narratives;
	}

	public void addNarrative(Narrative val) {
		narratives.add(val);
		val.setTopic(this);
	} 

}
