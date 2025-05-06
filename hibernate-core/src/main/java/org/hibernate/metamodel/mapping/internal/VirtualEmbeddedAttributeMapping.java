/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.VirtualModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.tree.from.TableGroupProducer;

/**
 * @author Christian Beikov
 */
public class VirtualEmbeddedAttributeMapping extends EmbeddedAttributeMapping implements VirtualModelPart {

	public VirtualEmbeddedAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			String tableExpression,
			AttributeMetadata attributeMetadata,
			String parentInjectionAttributeName,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				navigableRole,
				stateArrayPosition,
				fetchableIndex,
				tableExpression,
				attributeMetadata,
				parentInjectionAttributeName,
				mappedFetchTiming,
				mappedFetchStyle,
				embeddableMappingType,
				declaringType,
				propertyAccess
		);
	}

	public VirtualEmbeddedAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			int fetchableIndex,
			String tableExpression,
			AttributeMetadata attributeMetadata,
			PropertyAccess parentInjectionAttributePropertyAccess,
			FetchTiming mappedFetchTiming,
			FetchStyle mappedFetchStyle,
			EmbeddableMappingType embeddableMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				navigableRole,
				stateArrayPosition,
				fetchableIndex,
				tableExpression,
				attributeMetadata,
				parentInjectionAttributePropertyAccess,
				mappedFetchTiming,
				mappedFetchStyle,
				embeddableMappingType,
				declaringType,
				propertyAccess
		);
	}

	// Constructor is only used for creating the inverse attribute mapping
	VirtualEmbeddedAttributeMapping(
			ManagedMappingType keyDeclaringType,
			TableGroupProducer declaringTableGroupProducer,
			SelectableMappings selectableMappings,
			EmbeddableValuedModelPart inverseModelPart,
			EmbeddableMappingType embeddableTypeDescriptor,
			MappingModelCreationProcess creationProcess) {
		super(
				keyDeclaringType,
				declaringTableGroupProducer,
				selectableMappings,
				inverseModelPart,
				embeddableTypeDescriptor,
				creationProcess
		);
	}

}
