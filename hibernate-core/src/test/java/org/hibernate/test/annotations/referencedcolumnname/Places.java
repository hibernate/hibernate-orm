/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.referencedcolumnname;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

/**
 * @author Janario Oliveira
 */
@Embeddable
public class Places {

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumns({ @JoinColumn(name = "LIVING_ROOM", referencedColumnName = "NAME"),
			@JoinColumn(name = "LIVING_ROOM_OWNER", referencedColumnName = "OWNER") })
	Place livingRoom;
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "KITCHEN", referencedColumnName = "NAME")
	Place kitchen;
}
