/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embedded.spi;

import java.util.Collections;
import java.util.List;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.mapping.Component;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.TypeExporter;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.convert.spi.TableGroupProducer;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;
import org.hibernate.type.spi.EmbeddedType;

/**
 * Mapping for an embedded value.  Represents a specific usage of an embeddable/composite
 *
 * @author Steve Ebersole
 */
public interface EmbeddedPersister<T>
		extends ManagedTypeImplementor<T>, TypeExporter, EmbeddedContainer<T>, EmbeddableType<T>, EmbeddedReference<T> {
	Class[] STANDARD_CTOR_SIGNATURE = new Class[] {
			Component.class,
			EmbeddedContainer.class,
			String.class,
			PersisterCreationContext.class
	};

	void afterInitialization(
			Component embeddableBinding,
			PersisterCreationContext creationContext);

	default String getRoleName() {
		return getNavigableRole().getFullPath();
	}

	List<Column> collectColumns();

	@Override
	EmbeddableJavaDescriptor<T> getJavaTypeDescriptor();

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	EmbeddedContainer<?> getSource();

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
	default List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute) {
		return Collections.emptyList();
	}

	@Override
	default String asLoggableText() {
		return "EmbeddableMapper(" + getTypeName() + " [" + getRoleName() + "])";
	}
}
