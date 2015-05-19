/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Iterator;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.annotations.TableBinder;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;

/**
 * Enable a proper set of the FK columns in respect with the id column order
 * Allow the correct implementation of the default EJB3 values which needs both
 * sides of the association to be resolved
 *
 * @author Emmanuel Bernard
 */
public class ToOneFkSecondPass extends FkSecondPass {
	private MetadataBuildingContext buildingContext;
	private boolean unique;
	private String path;
	private String entityClassName;

	public ToOneFkSecondPass(
			ToOne value,
			Ejb3JoinColumn[] columns,
			boolean unique,
			String entityClassName,
			String path,
			MetadataBuildingContext buildingContext) {
		super( value, columns );
		this.buildingContext = buildingContext;
		this.unique = unique;
		this.entityClassName = entityClassName;
		this.path = entityClassName != null ? path.substring( entityClassName.length() + 1 ) : path;
	}

	@Override
    public String getReferencedEntityName() {
		return ( (ToOne) value ).getReferencedEntityName();
	}

	@Override
    public boolean isInPrimaryKey() {
		if ( entityClassName == null ) return false;
		final PersistentClass persistentClass = buildingContext.getMetadataCollector().getEntityBinding( entityClassName );
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
			//embedded property starts their path with 'id.' See PropertyPreloadedData( ) use when idClass != null in AnnotationSourceProcessor
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
			BinderHelper.createSyntheticPropertyReference( columns, ref, null, manyToOne, false, buildingContext );
			TableBinder.bindFk( ref, null, columns, manyToOne, unique, buildingContext );
			/*
			 * HbmMetadataSourceProcessorImpl does this only when property-ref != null, but IMO, it makes sense event if it is null
			 */
			if ( !manyToOne.isIgnoreNotFound() ) manyToOne.createPropertyRefConstraints( persistentClasses );
		}
		else if ( value instanceof OneToOne ) {
			value.createForeignKey();
		}
		else {
			throw new AssertionFailure( "FkSecondPass for a wrong value type: " + value.getClass().getName() );
		}
	}
}
