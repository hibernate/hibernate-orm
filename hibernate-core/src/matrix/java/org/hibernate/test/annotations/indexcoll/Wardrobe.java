//$Id$
package org.hibernate.test.annotations.indexcoll;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.IndexColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Wardrobe {

	private Long id;
	private List<Drawer> drawers;

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * unidirectional one to many list  with non null foreign key (mapping
	 * not recommended).
	 */
	@OneToMany(cascade = CascadeType.ALL)
	@IndexColumn(name = "drawer_position", base = 1)
	@JoinColumn(name = "wardrobe_id", nullable = false)
	public List<Drawer> getDrawers() {
		return drawers;
	}

	public void setDrawers(List<Drawer> drawers) {
		this.drawers = drawers;
	}
}
