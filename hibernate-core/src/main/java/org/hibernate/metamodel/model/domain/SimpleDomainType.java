package org.hibernate.metamodel.model.domain;

import jakarta.annotation.Nonnull;
import jakarta.persistence.metamodel.Type;

/**
 * Describes any non-collection type.
 *
 * @author Steve Ebersole
 */
public interface SimpleDomainType<J> extends DomainType<J>, Type<J> {
	@Override
	@Nonnull
	default Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}
}
