/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.accesstype;

import javax.persistence.Embeddable;
import javax.persistence.OneToOne;

/**
 * @author gsmet
 */
@Embeddable
public class Hotel {

	@OneToOne
	private User webDomainExpert;

	public User getWebDomainExpert() {
		return webDomainExpert;
	}

	public void setWebDomainExpert(User webDomainExpert) {
		this.webDomainExpert = webDomainExpert;
	}
}
