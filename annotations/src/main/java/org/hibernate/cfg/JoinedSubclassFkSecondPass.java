//$Id$
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.mapping.JoinedSubclass;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Emmanuel Bernard
 */
@SuppressWarnings({"serial", "unchecked"})
public class JoinedSubclassFkSecondPass extends FkSecondPass {
	private JoinedSubclass entity;
	private ExtendedMappings mappings;

	public JoinedSubclassFkSecondPass(JoinedSubclass entity, Ejb3JoinColumn[] inheritanceJoinedColumns, SimpleValue key, ExtendedMappings mappings) {
		super( key, inheritanceJoinedColumns );
		this.entity = entity;
		this.mappings = mappings;
	}

	public String getReferencedEntityName() {
		return entity.getSuperclass().getEntityName();
	}

	public boolean isInPrimaryKey() {
		return true;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		TableBinder.bindFk( entity.getSuperclass(), entity, columns, value, false, mappings );
	}
}
