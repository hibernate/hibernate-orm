/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.cascade;
import java.util.Collection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.CascadeType.DETACH;
import static jakarta.persistence.CascadeType.REMOVE;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Mouth {
	@Id
	@GeneratedValue
	public Integer id;
	@Column(name="mouth_size")
	public int size;
	@OneToMany(mappedBy = "mouth", cascade = { REMOVE, DETACH } )
	public Collection<Tooth> teeth;
}
