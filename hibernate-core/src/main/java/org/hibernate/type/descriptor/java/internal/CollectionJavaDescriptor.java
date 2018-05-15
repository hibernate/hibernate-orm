/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.java.internal;

import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.CollectionMutabilityPlan;
import org.hibernate.type.descriptor.java.spi.AbstractBasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class CollectionJavaDescriptor<C> extends AbstractBasicJavaDescriptor<C> {
	private final CollectionSemantics<C> semantics;

	public CollectionJavaDescriptor(
			Class<? extends C> type,
			CollectionSemantics<C> semantics) {
		super( type );
		this.semantics = semantics;
	}

	@Override
	public String getTypeName() {
		return getJavaType().getName();
	}

	@Override
	public CollectionMutabilityPlan<C> getMutabilityPlan() {
		return (CollectionMutabilityPlan<C>) super.getMutabilityPlan();
	}

	@Override
	public SqlTypeDescriptor getJdbcRecommendedSqlType(JdbcRecommendedSqlTypeMappingContext context) {
		// none
		return null;
	}

	@Override
	public String toString(C value) {
		return "CollectionJavaDescriptor(" + getTypeName() + ")";
	}

	@Override
	public C fromString(String string) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> X unwrap(C value, Class<X> type, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	public <X> C wrap(X value, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException(  );
	}

	public CollectionSemantics<C> getSemantics() {
		return semantics;
	}
}
