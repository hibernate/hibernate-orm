/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.Type;

/**
 * NOTE: Consider this contract (and its sub-contracts) as incubating as we transition to 6.0 and SQM
 *
 * @author Steve Ebersole
 */
@Incubating
public abstract class AbstractParameterDescriptor implements QueryParameter {
	private final int[] sourceLocations;

	private AllowableParameterType expectedType;

	public AbstractParameterDescriptor(int[] sourceLocations, AllowableParameterType expectedType) {
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
	public Class getParameterType() {
		return expectedType == null ? null : expectedType.getExpressableJavaTypeDescriptor().getJavaType();
	}

	@Override
	public AllowableParameterType getHibernateType() {
		return getExpectedType();
	}


	public AllowableParameterType getExpectedType() {
		return expectedType;
	}

	public void resetExpectedType(AllowableParameterType expectedType) {
		this.expectedType = expectedType;
	}
}
