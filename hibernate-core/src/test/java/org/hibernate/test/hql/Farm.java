/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.hql;

import java.util.List;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;

/**
 * @author Brett Meyer
 */
@Entity
public class Farm {

	@Id @GeneratedValue
	private long id;
	
	private String name;

	@ManyToMany(cascade = CascadeType.ALL)
	private List<Crop> crops;

	@ElementCollection
	@Enumerated
	@CollectionTable( name = "farm_accreditations" )
	private Set<Accreditation> accreditations;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Crop> getCrops() {
		return crops;
	}

	public void setCrops(List<Crop> crops) {
		this.crops = crops;
	}

	public Set<Accreditation> getAccreditations() {
		return accreditations;
	}

	public void setAccreditations(Set<Accreditation> accreditations) {
		this.accreditations = accreditations;
	}

	public static enum Accreditation {
		ORGANIC,
		SUSTAINABLE,
		FARM_TO_TABLE
	}
}
