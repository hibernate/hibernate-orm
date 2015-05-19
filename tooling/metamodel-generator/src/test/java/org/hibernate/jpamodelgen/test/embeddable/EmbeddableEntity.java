/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.embeddable;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;

/* Here the getter is mandatory to reproduce the issue. No @Access(FIELD) annotation. */
@Embeddable
//@Access(AccessType.FIELD)
public class EmbeddableEntity {
	@OneToMany(targetEntity = Stuff.class)
	Set<IStuff> stuffs = new HashSet<IStuff>();

	public Set<IStuff> getStuffs() {
		return stuffs;
	}
}
