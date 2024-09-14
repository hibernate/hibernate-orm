/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Emmanuel Bernard
 */
public class JoinedSubclassFkSecondPass extends FkSecondPass {
	private final JoinedSubclass entity;
	private final MetadataBuildingContext buildingContext;

	public JoinedSubclassFkSecondPass(
			JoinedSubclass entity,
			AnnotatedJoinColumns inheritanceJoinedColumns,
			SimpleValue key,
			MetadataBuildingContext buildingContext) {
		super( key, inheritanceJoinedColumns );
		this.entity = entity;
		this.buildingContext = buildingContext;
	}

	@Override
	public String getReferencedEntityName() {
		return entity.getSuperclass().getEntityName();
	}

	@Override
	public boolean isInPrimaryKey() {
		return true;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		TableBinder.bindForeignKey( entity.getSuperclass(), entity, columns, value, false, buildingContext );
	}
}
