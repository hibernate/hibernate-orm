//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Painter {
	private Integer id;
	private Map<String, Painting> paintings = new HashMap<String, Painting>();

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@OneToMany(cascade = {CascadeType.ALL})
	@MapKey(name = "name")
	@JoinColumn
	public Map<String, Painting> getPaintings() {
		return paintings;
	}

	public void setPaintings(Map<String, Painting> paintings) {
		this.paintings = paintings;
	}
}
