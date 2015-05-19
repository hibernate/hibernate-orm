/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.various;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Antenna {
	@Id public Integer id;
	@Generated(GenerationTime.ALWAYS) @Column()
	public String longitude;

	@Generated(GenerationTime.INSERT) @Column(insertable = false)
	public String latitude;

	@Generated(GenerationTime.NEVER)
	public Double power;
}
