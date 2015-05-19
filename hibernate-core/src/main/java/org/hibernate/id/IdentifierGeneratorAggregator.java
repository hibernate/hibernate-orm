/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.boot.model.relational.ExportableProducer;

/**
 * Identifies {@link IdentifierGenerator generators} which potentially aggregate other
 * {@link PersistentIdentifierGenerator} generators.
 * <p/>
 * Initially this is limited to {@link CompositeNestedGeneratedValueGenerator}
 *
 * @author Steve Ebersole
 */
public interface IdentifierGeneratorAggregator extends ExportableProducer {
}
