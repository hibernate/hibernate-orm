/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.various;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Antenna {
	@Id public Integer id;
	@Generated(event = { EventType.INSERT, EventType.UPDATE})
	@Column()
	public String longitude;

	@Generated(event = EventType.INSERT)
	@Column(insertable = false)
	public String latitude;

	public Double power;
}
