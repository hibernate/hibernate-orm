/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.animal;

import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;

/**
 * @author Steve Ebersole
 */
public class AnimalDomainModel extends AbstractDomainModelDescriptor {
	/**
	 * Singleton access
	 */
	public static final AnimalDomainModel INSTANCE = new AnimalDomainModel();

	public static void applyContactsModel(MetadataSources sources) {
		INSTANCE.applyDomainModel( sources );
	}

	public AnimalDomainModel() {
		super(
				Address.class,
				Animal.class,
				Cat.class,
				Classification.class,
				Dog.class,
				DomesticAnimal.class,
				Human.class,
				Lizard.class,
				Mammal.class,
				Name.class,
				PettingZoo.class,
				Reptile.class,
				StateProvince.class,
				Zoo.class
		);
	}
}
