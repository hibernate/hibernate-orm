//$Id$
package org.hibernate.test.annotations.xml.hbm;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

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
