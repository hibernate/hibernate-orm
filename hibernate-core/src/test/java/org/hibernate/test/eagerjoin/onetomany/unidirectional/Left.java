/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.eagerjoin.onetomany.unidirectional;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Pit Humke
 */
@Entity
@Table(name = "Left")
public class Left {
	@Id
	public String id;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(FetchMode.JOIN)
	@JoinColumn(name = "parent", foreignKey = @ForeignKey(value = ConstraintMode.NO_CONSTRAINT))
	Set<Bottom> bottom = new HashSet<>();
}
