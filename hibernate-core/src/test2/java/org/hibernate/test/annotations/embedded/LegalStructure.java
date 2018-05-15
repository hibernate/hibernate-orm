/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.embedded;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

/**
 * @author Emmanuel Bernard
 */
@Embeddable
public class LegalStructure {
	private String name;
	private String country;
	private CorpType corporationType;
	private Nationality origin;
	private Set<Manager> topManagement = new HashSet<Manager>();

	@ManyToOne
	@JoinColumn(name = "CORP_ID")
	public CorpType getCorporationType() {
		return corporationType;
	}

	public void setCorporationType(CorpType corporationType) {
		this.corporationType = corporationType;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@ManyToOne
	@JoinColumn(name = "origin_fk")
	public Nationality getOrigin() {
		return origin;
	}

	public void setOrigin(Nationality origin) {
		this.origin = origin;
	}

	@OneToMany(mappedBy = "employer")
	public Set<Manager> getTopManagement() {
		return topManagement;
	}

	public void setTopManagement(Set<Manager> topManagement) {
		this.topManagement = topManagement;
	}
}
