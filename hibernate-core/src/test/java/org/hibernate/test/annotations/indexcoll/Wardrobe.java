package org.hibernate.test.annotations.indexcoll;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;

import org.hibernate.annotations.ListIndexBase;

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
	@JoinColumn(name = "wardrobe_id", nullable = false)
	@OrderColumn( name = "drawer_position" )
	@ListIndexBase( 1 )
	public List<Drawer> getDrawers() {
		return drawers;
	}

	public void setDrawers(List<Drawer> drawers) {
		this.drawers = drawers;
	}
}
