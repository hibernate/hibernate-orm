/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.results.graph.Fetchable;

/**
 * Describes a ModelPart which is a basic value, either<ul>
 *     <li>a {@link jakarta.persistence.Basic} attribute</li>
 *     <li>a basic-valued collection part</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface BasicValuedModelPart extends BasicValuedMapping, ValuedModelPart, Fetchable, SelectableMapping {

	@Override
	default MappingType getPartMappingType() {
		return this::getJavaType;
	}

	@Override
	default int getJdbcTypeCount() {
		return 1;
	}

	@Override
	default JdbcMapping getJdbcMapping(int index) {
		return BasicValuedMapping.super.getJdbcMapping( index );
	}

	@Override
	default JdbcMapping getSingleJdbcMapping() {
		return BasicValuedMapping.super.getSingleJdbcMapping();
	}

	@Override
	default SelectableMapping getSelectable(int columnIndex) {
		return this;
	}

	@Override
	default int forEachSelectable(int offset, SelectableConsumer consumer) {
		consumer.accept( offset, this );
		return getJdbcTypeCount();
	}

	@Override
	default int forEachSelectable(SelectableConsumer consumer) {
		consumer.accept( 0, this );
		return getJdbcTypeCount();
	}

	@Override
	default boolean hasPartitionedSelectionMapping() {
		return isPartitioned();
	}

	@Override
	default BasicValuedModelPart asBasicValuedModelPart() {
		return this;
	}
}
