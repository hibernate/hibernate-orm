/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.referencedcolumnname;

import javax.persistence.AssociationOverride;
import javax.persistence.AssociationOverrides;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;

/**
 * @author Janario Oliveira
 */
@Entity
public class HousePlaces {

	@Id
	@GeneratedValue
	int id;
	@Embedded
	Places places;
	@Embedded
	@AssociationOverrides({
			@AssociationOverride(name = "livingRoom", joinColumns = {
					@JoinColumn(name = "NEIGHBOUR_LIVINGROOM", referencedColumnName = "NAME"),
					@JoinColumn(name = "NEIGHBOUR_LIVINGROOM_OWNER", referencedColumnName = "OWNER") }),
			@AssociationOverride(name = "kitchen", joinColumns = @JoinColumn(name = "NEIGHBOUR_KITCHEN", referencedColumnName = "NAME")) })
	Places neighbourPlaces;
}
