/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.persistence.metamodel.IdentifiableType;

import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.JoinedTableBinding;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * Hibernate extension SPI for working with {@link IdentifiableType} implementations, which includes
 * both mapped-superclasses {@link MappedSuperclassTypeDescriptor}
 * and {@link EntityTypeDescriptor}
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeDescriptor<T> extends InheritanceCapable<T>, IdentifiableDomainType<T> {
	@Override
	default IdentifiableTypeDescriptor<? super T> getSupertype() {
		return getSuperclassType();
	}

	@Override
	IdentifiableTypeDescriptor<? super T> getSuperclassType();

	@Override
	SimpleTypeDescriptor<?> getIdType();

	EntityHierarchy getHierarchy();

	interface InFlightAccess<X> extends ManagedTypeDescriptor.InFlightAccess<X> {
		void addSubTypeDescriptor(IdentifiableTypeDescriptor subTypeDescriptor);
	}

	@Override
	InFlightAccess<T> getInFlightAccess();

	void visitSubTypeDescriptors(Consumer<IdentifiableTypeDescriptor<? extends T>> action);
	void visitAllSubTypeDescriptors(Consumer<IdentifiableTypeDescriptor<? extends T>> action);

	IdentifiableTypeDescriptor findMatchingSubTypeDescriptors(Predicate<IdentifiableTypeDescriptor<? extends T>> matcher);

	default void visitConstraintOrderedTables(BiConsumer<Table, List<Column>> tableConsumer) {
		for ( JoinedTableBinding secondaryTableBinding : getSecondaryTableBindings() ) {
			tableConsumer.accept(
					secondaryTableBinding.getReferringTable(),
					secondaryTableBinding.getJoinForeignKey().getColumnMappings().getReferringColumns()
			);
		}

		final Table primaryTable = getPrimaryTable();
		if ( primaryTable != null ) {
			tableConsumer.accept( primaryTable, (List) primaryTable.getPrimaryKey().getColumns() );
		}
	}

	/**
	 * Access to the root table for this type.
	 */
	default Table getPrimaryTable() {
		return null;
	}

	/**
	 * Access to all "declared" secondary table mapping info for this type, not including
	 * secondary tables defined for super-types nor sub-types
	 */
	default List<JoinedTableBinding> getSecondaryTableBindings() {
		return Collections.emptyList();
	}
}
