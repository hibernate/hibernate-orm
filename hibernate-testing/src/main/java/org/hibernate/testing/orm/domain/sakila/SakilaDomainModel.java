/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.sakila;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;
import org.hibernate.testing.orm.domain.MappingFeature;

import static org.hibernate.testing.orm.domain.MappingFeature.COLLECTION_TABLE;
import static org.hibernate.testing.orm.domain.MappingFeature.CONVERTER;
import static org.hibernate.testing.orm.domain.MappingFeature.JOIN_COLUMN;
import static org.hibernate.testing.orm.domain.MappingFeature.JOIN_TABLE;
import static org.hibernate.testing.orm.domain.MappingFeature.MANY_MANY;
import static org.hibernate.testing.orm.domain.MappingFeature.MANY_ONE;
import static org.hibernate.testing.orm.domain.MappingFeature.ONE_ONE;

/**
 * Main sample database in MySQL or MariaDB which models a fictional medium-sized store selling movie DVDs.
 *
 * @author Nathan Xu
 * @see <a href="https://dev.mysql.com/doc/sakila/en/"></a>
 */
public class SakilaDomainModel extends AbstractDomainModelDescriptor {
	public static final SakilaDomainModel INSTANCE = new SakilaDomainModel();

	private SakilaDomainModel() {
		super(
				Actor.class,
				Address.class,
				Category.class,
				City.class,
				Country.class,
				Customer.class,
				Film.class,
				Inventory.class,
				Language.class,
				Payment.class,
				Rental.class,
				Staff.class,
				Store.class
		);
	}

	public static void applyRetailModel(MetadataSources sources) {
		INSTANCE.applyDomainModel( sources );
	}

	@Override
	public EnumSet<MappingFeature> getMappingFeaturesUsed() {
		return EnumSet.of(
				CONVERTER,

				MANY_ONE,
				ONE_ONE,
				MANY_MANY,

				COLLECTION_TABLE,
				JOIN_TABLE,
				JOIN_COLUMN
		);
	}
}
