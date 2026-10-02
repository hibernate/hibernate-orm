package org.hibernate.id;

import org.hibernate.boot.model.relational.ExportableProducer;

/**
 * Identifies {@linkplain IdentifierGenerator generators} which potentially aggregate other
 * {@link PersistentIdentifierGenerator} generators.
 * <p>
 * Initially this is limited to {@link CompositeNestedGeneratedValueGenerator}.
 *
 * @author Steve Ebersole
 */
public interface IdentifierGeneratorAggregator extends ExportableProducer {
}
