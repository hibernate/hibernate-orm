/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Emmanuel Bernard
 */
public class PkDrivenByDefaultMapsIdSecondPass extends FkSecondPass {
	private final String referencedEntityName;
	private final AnnotatedJoinColumns columns;
	private final SimpleValue value;

	public PkDrivenByDefaultMapsIdSecondPass(String referencedEntityName, AnnotatedJoinColumns columns, SimpleValue value) {
		super( value, columns );
		this.referencedEntityName = referencedEntityName;
		this.columns = columns;
		this.value = value;
	}

	@Override
	public String getReferencedEntityName() {
		return referencedEntityName;
	}

	@Override
	public boolean isInPrimaryKey() {
		// @MapsId is not itself in the primary key,
		// so it's safe to simply process it after all the primary keys have been processed.
		return true;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		PersistentClass referencedEntity = persistentClasses.get( referencedEntityName );
		if ( referencedEntity == null ) {
			// TODO: much better error message if this is something that can really happen!
			throw new AnnotationException( "Unknown entity name '" + referencedEntityName + "'" );
		}
		TableBinder.linkJoinColumnWithValueOverridingNameIfImplicit(
				referencedEntity,
				referencedEntity.getKey(),
				columns,
				value);
	}
}
