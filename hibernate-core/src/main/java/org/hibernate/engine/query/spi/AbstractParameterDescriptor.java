/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import org.hibernate.Incubating;
import org.hibernate.query.AllowableParameterType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;
import org.hibernate.query.QueryParameter;

/**
 * NOTE: Consider this contract (and its sub-contracts) as incubating as we transition to 6.0 and SQM
 *
 * @author Steve Ebersole
 */
@Incubating
public abstract class AbstractParameterDescriptor<T> implements QueryParameter<T> {
	private final int[] sourceLocations;

	private AllowableParameterType<T> expectedType;

	public AbstractParameterDescriptor(int[] sourceLocations, AllowableParameterType<T> expectedType) {
		this.sourceLocations = sourceLocations;
		this.expectedType = expectedType;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Integer getPosition() {
		return null;
	}

	@Override
	public Class<T> getParameterType() {
		return expectedType == null ? null : expectedType.getBindableJavaType();
	}

	@Override
	public AllowableParameterType<T> getHibernateType() {
		return getExpectedType();
	}


	public AllowableParameterType<T> getExpectedType() {
		return expectedType;
	}

	public void resetExpectedType(AllowableParameterType<T> expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		if ( expectedType instanceof EntityTypeImpl ) {
			return ( (EntityTypeImpl<T>) expectedType ).getIdType() instanceof EmbeddableDomainType;
		}
		return expectedType instanceof EmbeddableDomainType;
	}
}
