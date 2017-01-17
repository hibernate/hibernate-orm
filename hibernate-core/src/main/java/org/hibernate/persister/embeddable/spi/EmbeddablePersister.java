/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embeddable.spi;

import java.util.Collections;
import java.util.List;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.mapping.Component;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.TypeExporter;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.convert.spi.TableGroupProducer;
import org.hibernate.type.spi.EmbeddedType;

/**
 * Mapping for an embedded value.  Represents a specific usage of an embeddable/composite
 *
 * @author Steve Ebersole
 */
public interface EmbeddablePersister<T>
		extends ManagedTypeImplementor<T>, TypeExporter, EmbeddableContainer<T>, EmbeddableType<T>, EmbeddableReference<T> {
	Class[] STANDARD_CTOR_SIGNATURE = new Class[] {
			Component.class,
			EmbeddableContainer.class,
			String.class,
			PersisterCreationContext.class
	};

	void afterInitialization(
			Component embeddableBinding,
			PersisterCreationContext creationContext);

	String getRoleName();

	List<Column> collectColumns();

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	EmbeddableContainer<?> getSource();

	@Override
	EmbeddedType getOrmType();

	@Override
	default EmbeddedType getExportedDomainType() {
		return getOrmType();
	}

	@Override
	default String getTypeName() {
		return getOrmType().getJavaTypeDescriptor().getTypeName();
	}

	@Override
	@SuppressWarnings("unchecked")
	default Class<T> getJavaType() {
		return (Class<T>) getOrmType().getJavaTypeDescriptor().getJavaType();
	}

	@Override
	default boolean canCompositeContainCollections() {
		return getSource().canCompositeContainCollections();
	}

	@Override
	default TableGroupProducer resolveTableGroupProducer() {
		return getSource().resolveTableGroupProducer();
	}

	@Override
	default List<JoinColumnMapping> resolveJoinColumnMappings(Attribute attribute) {
		return Collections.emptyList();
	}

	@Override
	default String asLoggableText() {
		return "EmbeddableMapper(" + getTypeName() + " [" + getRoleName() + "])";
	}
}
