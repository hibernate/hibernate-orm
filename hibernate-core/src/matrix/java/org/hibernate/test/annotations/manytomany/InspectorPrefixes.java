//$Id$
package org.hibernate.test.annotations.manytomany;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrimaryKeyJoinColumn;

/**
 * @author Emmanuel Bernard
 */
@Entity
@PrimaryKeyJoinColumn(name = "inspector_id")
class InspectorPrefixes extends Inspector {
	@Column(name = "prefixes", nullable = false)
	private String prefixes;
	@ManyToMany()
	@JoinTable(name = "deserted_area",
			joinColumns = @JoinColumn(name = "inspector_name", referencedColumnName = "name"),
			inverseJoinColumns = @JoinColumn(name = "area_id", referencedColumnName = "id"))
	private List<Zone> desertedAreas = new ArrayList<Zone>();

	@ManyToMany()
	@JoinTable(name = "inspector_prefixes_areas",
			joinColumns = @JoinColumn(name = "inspector_id", referencedColumnName = "inspector_id"),
			inverseJoinColumns = @JoinColumn(name = "area_id", referencedColumnName = "id"))
	private List<Zone> areas = new ArrayList<Zone>();

	InspectorPrefixes() {
	}

	InspectorPrefixes(String prefixes) {
		this.prefixes = prefixes;
	}

	public String getPrefixes() {
		return this.prefixes;
	}

	public List<Zone> getAreas() {
		return areas;
	}

	public List<Zone> getDesertedAreas() {
		return desertedAreas;
	}
}
