/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.AttributeContainer;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.JoinableAttribute;
import org.hibernate.persister.common.spi.SingularAttribute;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.sql.convert.spi.TableGroupProducer;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.CompositeType;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeEmbedded
		extends AbstractSingularAttribute<CompositeType>
		implements SingularAttribute, CompositeReference, JoinableAttribute {

	private final CompositeContainer compositeContainer;
	private final EmbeddablePersister embeddablePersister;

	public SingularAttributeEmbedded(
			AttributeContainer declaringType,
			CompositeContainer compositeContainer,
			String attributeName,
			EmbeddablePersister embeddablePersister) {
		super( declaringType, attributeName, embeddablePersister.getOrmType(), true );
		this.compositeContainer = compositeContainer;
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public CompositeContainer getCompositeContainer() {
		return compositeContainer;
	}

	@Override
	public TableGroupProducer resolveTableGroupProducer() {
		return getCompositeContainer().resolveTableGroupProducer();
	}

	@Override
	public boolean canCompositeContainCollections() {
		return embeddablePersister.canCompositeContainCollections();
	}

	public EmbeddablePersister getEmbeddablePersister() {
		return embeddablePersister;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public List<Column> getColumns() {
		return embeddablePersister.collectColumns();
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}

	@Override
	public List<JoinColumnMapping> getJoinColumnMappings() {
		// there are no columns involved in a join to an embedded/composite attribute
		return Collections.emptyList();
	}
}
