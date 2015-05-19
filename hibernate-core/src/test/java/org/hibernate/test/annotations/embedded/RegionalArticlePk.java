/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.embedded;
import java.io.Serializable;
import javax.persistence.Embeddable;

import org.hibernate.annotations.AccessType;

/**
 * Regional article pk
 *
 * @author Emmanuel Bernard
 */
@Embeddable
@AccessType("field")
public class RegionalArticlePk implements Serializable {
	/**
	 * country iso2 code
	 */
	public String iso2;
	public String localUniqueKey;

	public int hashCode() {
		//this implem sucks
		return ( iso2 + localUniqueKey ).hashCode();
	}

	public boolean equals(Object obj) {
		//iso2 and localUniqueKey are expected to be set in this implem
		if ( obj != null && obj instanceof RegionalArticlePk ) {
			RegionalArticlePk other = (RegionalArticlePk) obj;
			return iso2.equals( other.iso2 ) && localUniqueKey.equals( other.localUniqueKey );
		}
		else {
			return false;
		}
	}
}
