/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class NullSqmExpressible implements SqmExpressible<Object> {
	/**
	 * Singleton access
	 */
	public static final NullSqmExpressible NULL_SQM_EXPRESSIBLE = new NullSqmExpressible();

	@Override
	public Class<Object> getBindableJavaType() {
		return null;
	}

	@Override
	public JavaType<Object> getExpressibleJavaType() {
		return null;
	}

	@Override
	public DomainType<Object> getSqmType() {
		return null;
	}
}
