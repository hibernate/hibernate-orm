package org.hibernate.query.sqm.tree.spi;

import java.io.Serializable;

import jakarta.annotation.Nonnull;
import org.hibernate.query.sqm.spi.NodeBuilder;

/**
 * Base implementation of a criteria node.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractSqmNode implements SqmNode, Serializable {
	private final @Nonnull NodeBuilder builder;

	protected AbstractSqmNode(@Nonnull NodeBuilder builder) {
		this.builder = builder;
	}

	@Override
	public @Nonnull NodeBuilder nodeBuilder() {
		return builder;
	}
}
