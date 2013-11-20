/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.loader.plan.build.internal.spaces;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.loader.plan.spi.CollectionQuerySpace;
import org.hibernate.loader.plan.spi.CompositeQuerySpace;
import org.hibernate.loader.plan.spi.EntityQuerySpace;
import org.hibernate.loader.plan.spi.JoinDefinedByMetadata;
import org.hibernate.loader.plan.spi.QuerySpace;
import org.hibernate.persister.entity.Joinable;
import org.hibernate.persister.entity.PropertyMapping;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.EntityType;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class JoinHelper {
	/**
	 * Singleton access
	 */
	public static final JoinHelper INSTANCE = new JoinHelper();

	private JoinHelper() {
	}

	public JoinDefinedByMetadata createEntityJoin(
			QuerySpace leftHandSide,
			String lhsPropertyName,
			EntityQuerySpace rightHandSide,
			boolean rightHandSideRequired,
			EntityType joinedPropertyType,
			SessionFactoryImplementor sessionFactory) {
		return new JoinImpl(
				leftHandSide,
				lhsPropertyName,
				rightHandSide,
				determineRhsColumnNames( joinedPropertyType, sessionFactory ),
				joinedPropertyType,
				rightHandSideRequired
		);
	}

	public JoinDefinedByMetadata createCollectionJoin(
			QuerySpace leftHandSide,
			String lhsPropertyName,
			CollectionQuerySpace rightHandSide,
			boolean rightHandSideRequired,
			CollectionType joinedPropertyType,
			SessionFactoryImplementor sessionFactory) {
		return new JoinImpl(
				leftHandSide,
				lhsPropertyName,
				rightHandSide,
				joinedPropertyType.getAssociatedJoinable( sessionFactory ).getKeyColumnNames(),
				joinedPropertyType,
				rightHandSideRequired
		);
	}

	public JoinDefinedByMetadata createCompositeJoin(
			QuerySpace leftHandSide,
			String lhsPropertyName,
			CompositeQuerySpace rightHandSide,
			boolean rightHandSideRequired,
			CompositeType joinedPropertyType) {
		return new JoinImpl(
				leftHandSide,
				lhsPropertyName,
				rightHandSide,
				null,
				joinedPropertyType,
				rightHandSideRequired
		);
	}

	private static String[] determineRhsColumnNames(EntityType entityType, SessionFactoryImplementor sessionFactory) {
		final Joinable persister = entityType.getAssociatedJoinable( sessionFactory );
		return entityType.getRHSUniqueKeyPropertyName() == null ?
				persister.getKeyColumnNames() :
				( (PropertyMapping) persister ).toColumns( entityType.getRHSUniqueKeyPropertyName() );
	}
}
