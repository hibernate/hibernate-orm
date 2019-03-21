/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Map;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.metamodel.model.domain.CollectionDomainType;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.produce.spi.TableReferenceContributor;

/**
 * @author Steve Ebersole
 */
public interface CollectionElement<J> extends Navigable<J>, CollectionDomainType.Element<J>, TableReferenceContributor {
	String NAVIGABLE_NAME = "{element}";

	boolean canContainSubGraphs();

	enum ElementClassification {
		BASIC( PersistenceType.BASIC ),
		EMBEDDABLE( PersistenceType.EMBEDDABLE ),
		ANY( null ),
		ONE_TO_MANY( PersistenceType.ENTITY ),
		MANY_TO_MANY( PersistenceType.ENTITY );

		private final PersistenceType correspondingJpaPersistenceType;

		ElementClassification(PersistenceType correspondingJpaPersistenceType) {
			this.correspondingJpaPersistenceType = correspondingJpaPersistenceType;
		}

		public PersistenceType getCorrespondingJpaPersistenceType() {
			return correspondingJpaPersistenceType;
		}
	}

	ElementClassification getClassification();

	PersistentCollectionDescriptor getCollectionDescriptor();

	Table getPrimaryDmlTable();

	SimpleTypeDescriptor getDomainTypeDescriptor();

	@Override
	default Class getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	boolean hasNotNullColumns();

	boolean isMutable();

	// todo (6.0) - should this be moved into a super contract?
	default J replace(J originalValue, J targetValue, Object owner, Map copyCache, SessionImplementor session) {
		return getJavaTypeDescriptor().getMutabilityPlan().replace(
				this,
				originalValue,
				targetValue,
				owner,
				copyCache,
				session
		);
	}
}
