/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
