/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.override;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.ManyToMany;

@Embeddable
public class SocialTouchPoints {

	// owning side of many to many
	@ManyToMany(cascade= CascadeType.ALL)
	List<SocialSite> website;

	public List<SocialSite> getWebsite() {
		return website;
	}

	public void setWebsite(List<SocialSite> website) {
		this.website = website;
	}
}
