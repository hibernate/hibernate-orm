/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.embedded.spi;

import java.util.List;
import javax.persistence.metamodel.EmbeddableType;

import org.hibernate.boot.model.domain.EmbeddedValueMapping;
import org.hibernate.mapping.Component;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.queryable.spi.NavigableReferenceInfo;
import org.hibernate.persister.queryable.spi.TableGroupResolver;
import org.hibernate.persister.spi.PersisterCreationContext;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.type.descriptor.java.spi.EmbeddableJavaDescriptor;

/**
 * Mapping for an embedded value.  Represents a specific usage of an embeddable/composite
 *
 * @author Steve Ebersole
 */
public interface EmbeddedPersister<T>
		extends ManagedTypeImplementor<T>, EmbeddedContainer<T>, EmbeddableType<T>,
		EmbeddedValuedNavigable<T> {

	Class[] STANDARD_CTOR_SIGNATURE = new Class[] {
			Component.class,
			EmbeddedContainer.class,
			String.class,
			PersisterCreationContext.class
	};

	/**
	 * Called after all managed types in the persistence unit have been
	 * instantiated.  Some will have been at least partially populated.
	 * <p/>
	 * At this point implementors ought to be able to locate other
	 * managed types.
	 *
	 * @param embeddedValueMapping The source boot-time mapping descriptor
	 * @param creationContext Access to services needed while finishing the instantiation
	 */
	void finishInstantiation(
			EmbeddedValueMapping embeddedValueMapping,
			PersisterCreationContext creationContext);

	/**
	 * Called after all managed types in the persistence unit have been
	 * fully instantiated (all their `#finishInstantiation` calls have completed).
	 * <p/>
	 * At this point implementors ought to be able to rely on the complete Navigable structure to have
	 * at least been "stitched" together and most information has been populated (there are a few
	 * exceptions, which we ought to list in a Navigable "design doc").
	 *
	 * todo (6.0) : Create Navigable-design-doc either here in repo or as wiki
	 *
	 * @param embeddedValueMapping The source boot-time mapping descriptor
	 * @param creationContext Access to services needed while finishing the instantiation
	 */
	void completeInitialization(
			EmbeddedValueMapping embeddedValueMapping,
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
	EmbeddedContainer<?> getContainer();

	@Override
	default String getTypeName() {
		return getContainer().getJavaTypeDescriptor().getTypeName();
	}

	@Override
	@SuppressWarnings("unchecked")
	default Class<T> getJavaType() {
		return (Class<T>) getContainer().getJavaTypeDescriptor().getJavaType();
	}

	@Override
	default boolean canCompositeContainCollections() {
		return getContainer().canCompositeContainCollections();
	}

	@Override
	default TableGroup resolveTableGroup(NavigableReferenceInfo embeddedReferenceInfo, TableGroupResolver tableGroupResolver) {
		return getContainer().resolveTableGroup( embeddedReferenceInfo, tableGroupResolver );
	}

	@Override
	default String asLoggableText() {
		return "EmbeddableMapper(" + getTypeName() + " [" + getRoleName() + "])";
	}
}
