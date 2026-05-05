/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.AssertionFailure;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.sql.model.TableMapping;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface EntityTableMapping extends TableMapping {
	static EntityTableMappingImpl.KeyMapping createKeyMapping(List<EntityTableMappingImpl.KeyColumn> keyColumns, ModelPart identifierPart) {
		if ( identifierPart instanceof EmbeddableValuedModelPart embeddedModelPart ) {
			return new EntityTableMappingImpl.CompositeKeyMapping( keyColumns, embeddedModelPart );
		}
		else if ( identifierPart instanceof BasicValuedModelPart basicModelPart ) {
			assert keyColumns.size() == 1;
			return new EntityTableMappingImpl.SimpleKeyMapping( keyColumns, basicModelPart );
		}
		else {
			throw new AssertionFailure( "Unexpected identifier part type" );
		}
	}

	boolean isSecondaryTable();

	EntityTableMappingImpl.KeyMapping getKeyMapping();

	boolean hasColumns();

	boolean containsAttributeColumns(int attributeIndex);

	int[] getAttributeIndexes();

	Expectation getInsertExpectation();

	String getInsertCustomSql();

	boolean isInsertCallable();

	Expectation getUpdateExpectation();

	String getUpdateCustomSql();

	boolean isUpdateCallable();

	Expectation getDeleteExpectation();

	String getDeleteCustomSql();

	boolean isDeleteCallable();
}
