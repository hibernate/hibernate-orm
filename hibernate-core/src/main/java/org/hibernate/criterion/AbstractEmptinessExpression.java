/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.sql.ConditionFragment;

import net.bytebuddy.description.annotation.AnnotationDescription;

/**
 * Base expression implementation for (not) emptiness checking of collection properties
 *
 * @author Steve Ebersole
 */
public abstract class AbstractEmptinessExpression implements Criterion {

	private static final TypedValue[] NO_VALUES = new TypedValue[0];

	protected final String propertyName;

	protected AbstractEmptinessExpression(String propertyName) {
		this.propertyName = propertyName;
	}

	/**
	 * Should empty rows be excluded?
	 *
	 * @return {@code true} Indicates the expression should be 'exists'; {@code false} indicates 'not exists'
	 */
	protected abstract boolean excludeEmpty();

	@Override
	public final String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
		final String entityName = criteriaQuery.getEntityName( criteria, propertyName );
		final String actualPropertyName = criteriaQuery.getPropertyName( propertyName );
		final String sqlAlias = criteriaQuery.getSQLAlias( criteria, propertyName );

		final SessionFactoryImplementor factory = criteriaQuery.getFactory();
		final EntityDescriptor entityPersister = factory.getTypeConfiguration().findEntityDescriptor( entityName );

		final Navigable navigable = entityPersister.findNavigable( actualPropertyName );

		final PersistentCollectionDescriptor collectionPersister = factory.getTypeConfiguration()
				.findCollectionPersister( navigable.getNavigableRole().getFullPath() );

		final String[] collectionKeys = collectionPersister.getKeyColumnNames();
		final String[] ownerKeys = ( (AnnotationDescription.Loadable) entityPersister ).getIdentifierColumnNames();

		final String innerSelect = "(select 1 from " + collectionPersister.getTableName() + " where "
				+ new ConditionFragment().setTableAlias( sqlAlias ).setCondition( ownerKeys, collectionKeys ).toFragmentString()
				+ ")";

		return excludeEmpty()
				? "exists " + innerSelect
				: "not exists " + innerSelect;
	}

	@Override
	public final TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery)
			throws HibernateException {
		return NO_VALUES;
	}

	@Override
	public final String toString() {
		return propertyName + ( excludeEmpty() ? " is not empty" : " is empty" );
	}
}
