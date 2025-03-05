/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
