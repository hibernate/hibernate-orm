/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cfg;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.MappingException;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;

/**
 * @author Emmanuel Bernard
 */
public class PkDrivenByDefaultMapsIdSecondPass implements SecondPass {
	private final String referencedEntityName;
	private final Ejb3JoinColumn[] columns;
	private final SimpleValue value;

	public PkDrivenByDefaultMapsIdSecondPass(String referencedEntityName, Ejb3JoinColumn[] columns, SimpleValue value) {
		this.referencedEntityName = referencedEntityName;
		this.columns = columns;
		this.value = value;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		PersistentClass referencedEntity = (PersistentClass) persistentClasses.get( referencedEntityName );
		if ( referencedEntity == null ) {
			throw new AnnotationException(
					"Unknown entity name: " + referencedEntityName
			);
		};
		TableBinder.linkJoinColumnWithValueOverridingNameIfImplicit(
				referencedEntity,
				referencedEntity.getKey().getColumnIterator(),
				columns,
				value);
	}
}
