/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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

import java.util.Iterator;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.Component;
import org.hibernate.util.StringHelper;

/**
 * Enable a proper set of the FK columns in respect with the id column order
 * Allow the correct implementation of the default EJB3 values which needs both
 * sides of the association to be resolved
 *
 * @author Emmanuel Bernard
 */
public class ToOneFkSecondPass extends FkSecondPass {
	private boolean unique;
	private ExtendedMappings mappings;
	private String path;
	private String entityClassName;

	public ToOneFkSecondPass(
			ToOne value, Ejb3JoinColumn[] columns, boolean unique, String entityClassName, String path, ExtendedMappings mappings
	) {
		super( value, columns );
		this.mappings = mappings;
		this.unique = unique;
		this.entityClassName = entityClassName;
		this.path = entityClassName != null ? path.substring( entityClassName.length() + 1 ) : path;
	}

	public String getReferencedEntityName() {
		return ( (ToOne) value ).getReferencedEntityName();
	}

	public boolean isInPrimaryKey() {
		if ( entityClassName == null ) return false;
		final PersistentClass persistentClass = mappings.getClass( entityClassName );
		Property property = persistentClass.getIdentifierProperty();
		if ( path == null ) {
			return false;
		}
		else if ( property != null) {
			//try explicit identifier property
			return path.startsWith( property.getName() + "." );
		}
		else {
			//try the embedded property
			//embedded property starts their path with 'id.' See PropertyPreloadedData( ) use when idClass != null in AnnotationBinder
			if ( path.startsWith( "id." ) ) {
				KeyValue valueIdentifier = persistentClass.getIdentifier();
				String localPath = path.substring( 3 );
				if ( valueIdentifier instanceof Component ) {
					Iterator it = ( (Component) valueIdentifier ).getPropertyIterator();
					while ( it.hasNext() ) {
						Property idProperty = (Property) it.next();
						if ( localPath.startsWith( idProperty.getName() ) ) return true;
					}

				}
			}
		}
		return false;
	}

	public void doSecondPass(java.util.Map persistentClasses) throws MappingException {
		if ( value instanceof ManyToOne ) {
			ManyToOne manyToOne = (ManyToOne) value;
			PersistentClass ref = (PersistentClass) persistentClasses.get( manyToOne.getReferencedEntityName() );
			if ( ref == null ) {
				throw new AnnotationException(
						"@OneToOne or @ManyToOne on "
								+ StringHelper.qualify( entityClassName, path )
								+ " references an unknown entity: "
								+ manyToOne.getReferencedEntityName()
				);
			}
			BinderHelper.createSyntheticPropertyReference( columns, ref, null, manyToOne, false, mappings );
			TableBinder.bindFk( ref, null, columns, manyToOne, unique, mappings );
			/*
			 * HbmBinder does this only when property-ref != null, but IMO, it makes sense event if it is null
			 */
			if ( !manyToOne.isIgnoreNotFound() ) manyToOne.createPropertyRefConstraints( persistentClasses );
		}
		else if ( value instanceof OneToOne ) {
			( (OneToOne) value ).createForeignKey();
		}
		else {
			throw new AssertionFailure( "FkSecondPass for a wrong value type: " + value.getClass().getName() );
		}
	}
}
