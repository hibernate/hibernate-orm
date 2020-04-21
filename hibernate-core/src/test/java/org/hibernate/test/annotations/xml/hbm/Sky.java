/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.xml.hbm;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;

/**
 * @author Emmanuel Bernard
 */
@Entity(name="EarthSky")
public class Sky {
	private Integer id;
	private Set<CloudType> cloudTypes = new HashSet<CloudType>();
	private CloudType mainCloud;

	@ManyToMany
	public Set<CloudType> getCloudTypes() {
		return cloudTypes;
	}

	public void setCloudTypes(Set<CloudType> cloudTypes) {
		this.cloudTypes = cloudTypes;
	}

	@Id @GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@ManyToOne
	public CloudType getMainCloud() {
		return mainCloud;
	}

	public void setMainCloud(CloudType mainCloud) {
		this.mainCloud = mainCloud;
	}
}
