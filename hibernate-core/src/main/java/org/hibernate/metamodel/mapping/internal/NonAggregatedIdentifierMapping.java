/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;

/**
 * @author Steve Ebersole
 */
public interface NonAggregatedIdentifierMapping extends CompositeIdentifierMapping, EmbeddableValuedFetchable, FetchOptions {
	IdClassEmbeddable getIdClassEmbeddable();
	VirtualIdEmbeddable getVirtualIdEmbeddable();

	IdentifierValueMapper getIdentifierValueMapper();

	/**
	 * Think of an AttributeConverter for id values.  Handles representation
	 * difference between virtual and id-class mappings
	 */
	interface IdentifierValueMapper extends EmbeddableMappingType {
		EmbeddableValuedModelPart getEmbeddedPart();

		Object getIdentifier(Object entity, SharedSessionContractImplementor session);
		void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session);

		default void forEachAttribute(IndexedConsumer<SingularAttributeMapping> consumer) {
			getEmbeddedPart().getEmbeddableTypeDescriptor().forEachAttributeMapping( (IndexedConsumer) consumer );
		}
	}
}
