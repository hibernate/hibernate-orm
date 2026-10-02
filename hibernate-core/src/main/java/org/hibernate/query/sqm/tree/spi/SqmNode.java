package org.hibernate.query.sqm.tree.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.query.criteria.JpaCriteriaNode;
import org.hibernate.query.sqm.spi.NodeBuilder;

import org.jboss.logging.Logger;

/**
 * Base contract for any SQM AST node.
 *
 * @author Steve Ebersole
 */
public interface SqmNode extends JpaCriteriaNode, SqmCacheable {
	Logger LOG = Logger.getLogger( SqmNode.class );

	@Nonnull
	default String asLoggableText() {
		LOG.debugf( "#asLoggableText not defined for %s - using #toString", getClass().getName() );
		return toString();
	}

	@Nonnull
	NodeBuilder nodeBuilder();

	@Nonnull
	SqmNode copy(@Nonnull SqmCopyContext context);
}
