//$Id$
package org.hibernate.test.annotations.xml.ejb3;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.NamedQuery;
import javax.persistence.TableGenerator;

/**
 * @author Emmanuel Bernard
 */
@Entity
@NamedQuery(name="manufacturer.findAll", query = "from Manufacturer where 1 = 2")
@TableGenerator(name="generator", table = "this is a broken name with select from and other SQL keywords")
public class Manufacturer {
	private Integer id;
	private Set<Model> models = new HashSet<Model>();

	@Id
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany
	public Set<Model> getModels() {
		return models;
	}

	public void setModels(Set<Model> models) {
		this.models = models;
	}
}
