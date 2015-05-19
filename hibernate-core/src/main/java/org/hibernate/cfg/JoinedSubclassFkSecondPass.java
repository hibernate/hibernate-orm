/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({"serial", "unchecked"})
public class JoinedSubclassFkSecondPass extends FkSecondPass {
	private JoinedSubclass entity;
	private MetadataBuildingContext buildingContext;

	public JoinedSubclassFkSecondPass(
			JoinedSubclass entity,
			Ejb3JoinColumn[] inheritanceJoinedColumns,
			SimpleValue key,
			MetadataBuildingContext buildingContext) {
		super( key, inheritanceJoinedColumns );
		this.entity = entity;
		this.buildingContext = buildingContext;
	}

	public String getReferencedEntityName() {
		return entity.getSuperclass().getEntityName();
	}

	public boolean isInPrimaryKey() {
		return true;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		TableBinder.bindFk( entity.getSuperclass(), entity, columns, value, false, buildingContext );
	}
}
