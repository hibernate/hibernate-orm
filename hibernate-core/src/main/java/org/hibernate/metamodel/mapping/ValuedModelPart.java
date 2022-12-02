/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

/**
 * Describes a ModelPart that is also a ValueMapping (and therefore also a SelectableMappings).
 * <p/>
 * {@linkplain BasicValuedModelPart Basic} and {@linkplain EmbeddableValuedModelPart embedded}
 * model-parts fall into this category.
 *
 * @author Steve Ebersole
 */
public interface ValuedModelPart extends ModelPart, ValueMapping {
	/**
	 * The table which contains the columns mapped by this value
	 */
	String getContainingTableExpression();

	// todo (ValuedModelPart) : consider moving `#getContainingTableExpression` to `SelectableMappings`
	//		depends if we ever use this to group selectable-mappings from different tables.


	@Override
	default int getJdbcTypeCount() {
		return ModelPart.super.getJdbcTypeCount();
	}

	@Override
	default List<JdbcMapping> getJdbcMappings() {
		return ModelPart.super.getJdbcMappings();
	}

	@Override
	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		return ModelPart.super.forEachSelectable( offset, consumer );
	}

	@Override
	default int forEachSelectable(SelectableConsumer consumer) {
		return ModelPart.super.forEachSelectable( consumer );
	}
}
