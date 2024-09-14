/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.mapping;

/**
 * Describes a ModelPart that is also a ValueMapping (and therefore also a SelectableMappings).
 * <p/>
 * {@linkplain BasicValuedModelPart Basic} and {@linkplain EmbeddableValuedModelPart embedded}
 * model-parts fall into this category.
 *
 * @author Steve Ebersole
 */
public interface ValuedModelPart extends ModelPart, ValueMapping, SelectableMappings {
	/**
	 * The table which contains the columns mapped by this value
	 */
	String getContainingTableExpression();

	@Override
	default int getJdbcTypeCount() {
		return ModelPart.super.getJdbcTypeCount();
	}

	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return ModelPart.super.getSingleJdbcMapping();
	}

	@Override
	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		return ModelPart.super.forEachSelectable( offset, consumer );
	}

	@Override
	default int forEachSelectable(SelectableConsumer consumer) {
		return ModelPart.super.forEachSelectable( consumer );
	}

	default void forEachInsertable(SelectableConsumer consumer) {
		ModelPart.super.forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( ! selectableMapping.isInsertable() || selectableMapping.isFormula() ) {
						return;
					}

					consumer.accept( selectionIndex, selectableMapping );
				}
		);
	}

	default void forEachNonFormula(SelectableConsumer consumer) {
		ModelPart.super.forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( selectableMapping.isFormula() ) {
						return;
					}

					consumer.accept( selectionIndex, selectableMapping );
				}
		);
	}

	default void forEachUpdatable(SelectableConsumer consumer) {
		ModelPart.super.forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( ! selectableMapping.isUpdateable() || selectableMapping.isFormula() ) {
						return;
					}

					consumer.accept( selectionIndex, selectableMapping );
				}
		);
	}

}
