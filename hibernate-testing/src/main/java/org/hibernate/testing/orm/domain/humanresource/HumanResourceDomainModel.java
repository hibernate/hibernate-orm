/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.domain.humanresource;

import java.util.EnumSet;

import org.hibernate.boot.MetadataSources;
import org.hibernate.testing.orm.domain.AbstractDomainModelDescriptor;
import org.hibernate.testing.orm.domain.MappingFeature;

import static org.hibernate.testing.orm.domain.MappingFeature.AGG_COMP_ID;
import static org.hibernate.testing.orm.domain.MappingFeature.EMBEDDABLE;
import static org.hibernate.testing.orm.domain.MappingFeature.JOIN_COLUMN;
import static org.hibernate.testing.orm.domain.MappingFeature.MANY_ONE;

/**
 * Main example database in the Oracle distribution which models a Human Resources department.
 *
 * @author Nathan Xu
 * @see <a href="https://docs.oracle.com/cd/B13789_01/server.101/b10771/scripts003.htm"></a>
 */
public class HumanResourceDomainModel extends AbstractDomainModelDescriptor {
	public static final HumanResourceDomainModel INSTANCE = new HumanResourceDomainModel();

	private HumanResourceDomainModel() {
		super(
			Country.class,
			Department.class,
			Employee.class,
			Job.class,
			JobHistory.class,
			Location.class,
			Region.class
		);
	}

	public static void applyRetailModel(MetadataSources sources) {
		INSTANCE.applyDomainModel( sources );
	}

	@Override
	public EnumSet<MappingFeature> getMappingFeaturesUsed() {
		return EnumSet.of(
				EMBEDDABLE,
				AGG_COMP_ID,
				MANY_ONE,
				JOIN_COLUMN
		);
	}
}
