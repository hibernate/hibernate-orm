/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.inheritance.mixed.entities;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

@Audited
@Entity
@DiscriminatorValue(value = "NORMAL")
public class NormalActivity extends AbstractActivity {

	@Override
	public boolean equals(Object obj) {
		if ( obj == this ) {
			return true;
		}
		if ( !(obj instanceof NormalActivity) ) {
			return false;
		}
		NormalActivity normalActivity = (NormalActivity) obj;
		return getId().equals( normalActivity.getId() );
	}

	@Override
	public int hashCode() {
		return getId().hashCode();
	}
}
