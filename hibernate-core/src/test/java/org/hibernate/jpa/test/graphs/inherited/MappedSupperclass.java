/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.graphs.inherited;

import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.HashSet;
import java.util.Set;


/**
 * @author Oliver Breidenbach
 */
@MappedSuperclass
public class MappedSupperclass {
	@Id @GeneratedValue
	public long id;

    @OneToOne(fetch = FetchType.LAZY)
	public Bar bar;

	@OneToMany
	public Set<Bar> bars = new HashSet<Bar>();

}
