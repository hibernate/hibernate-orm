/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.cascade;
import java.util.Collection;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import static javax.persistence.CascadeType.DETACH;
import static javax.persistence.CascadeType.REMOVE;

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
