/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.boot.mapping.internal.materialize.DependentTableKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ForeignKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.PrimaryTableKeyMappingMaterializer;
import org.hibernate.boot.mapping.internal.materialize.ResolvedPrimaryTableKey;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

/**
 * @author Emmanuel Bernard
 */
public class CreateKeySecondPass implements SecondPass {
	private RootClass rootClass;
	private JoinedSubclass joinedSubClass;
	private final DependentTableKeyMappingMaterializer dependentTableKeyMappingMaterializer = new DependentTableKeyMappingMaterializer();
	private final ForeignKeyMappingMaterializer foreignKeyMappingMaterializer = new ForeignKeyMappingMaterializer();

	public CreateKeySecondPass(RootClass rootClass) {
		this.rootClass = rootClass;
	}

	public CreateKeySecondPass(JoinedSubclass joinedSubClass) {
		this.joinedSubClass = joinedSubClass;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) {
		if ( rootClass != null ) {
			new PrimaryTableKeyMappingMaterializer( rootClass.getMetadataBuildingContext() ).finalizePrimaryKey(
					new ResolvedPrimaryTableKey(
							rootClass,
							rootClass.getTable(),
							rootClass.getMetadataBuildingContext(),
							null,
							rootClass.getKey().getColumns()
					)
			);
		}
		else if ( joinedSubClass != null ) {
			dependentTableKeyMappingMaterializer.materializePrimaryKey(
					dependentTableKeyMappingMaterializer.resolvePrimaryKey(
							joinedSubClass,
							joinedSubClass.getEntityName(),
							joinedSubClass.getTable(),
							joinedSubClass.getKey()
					)
			);
			if ( joinedSubClass.isJoinedSubclass() ) {
				foreignKeyMappingMaterializer.materializeForeignKey(
						joinedSubClass.getKey(),
						joinedSubClass.getSuperclass(),
						joinedSubClass.getEntityName()
				);
			}
		}
		else {
			throw new AssertionError( "rootClass and joinedSubClass are null" );
		}
	}
}
