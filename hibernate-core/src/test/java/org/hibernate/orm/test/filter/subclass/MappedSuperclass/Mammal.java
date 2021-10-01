/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.filter.subclass.MappedSuperclass;

import org.hibernate.type.NumericBooleanConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

/**
 * @author Andrea Boriero
 */

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public class Mammal extends Animal {

	@Column(name = "IS_PREGNANT")
	@Convert( converter = NumericBooleanConverter.class )
	private boolean isPregnant;

	public boolean isPregnant() {
		return isPregnant;
	}

	public void setPregnant(boolean isPregnant) {
		this.isPregnant = isPregnant;
	}
}
