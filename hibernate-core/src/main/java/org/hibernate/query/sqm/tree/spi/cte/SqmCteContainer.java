package org.hibernate.query.sqm.tree.spi.cte;

import java.util.Collection;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.query.criteria.JpaCteContainer;
import org.hibernate.query.sqm.tree.spi.SqmNode;

/**
 * @author Christian Beikov
 */
public interface SqmCteContainer extends SqmNode, JpaCteContainer {

	@Nonnull
	Collection<SqmCteStatement<?>> getCteStatements();

	@Nullable
	SqmCteStatement<?> getCteStatement(@Nonnull String cteLabel);

}
