/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.function.Supplier;

import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.type.ArrayType;
import org.hibernate.type.BasicType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.usertype.UserCollectionType;

/**
 * An array mapping has a primary key consisting of the key columns + index column.
 *
 * @author Gavin King
 */
public class Array extends List {
	private String elementClassName;

	public Array(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );
	}

	public Array(Supplier<ManagedBean<? extends UserCollectionType>> customTypeBeanResolver, PersistentClass owner, MetadataBuildingContext buildingContext) {
		super( customTypeBeanResolver, owner, buildingContext );
	}

	protected Array(Array original) {
		super( original );
		this.elementClassName = original.elementClassName;
	}

	@Override
	public Array copy() {
		return new Array( this );
	}

	public Class<?> getElementClass() throws MappingException {
		if ( elementClassName == null ) {
			final org.hibernate.type.Type elementType = getElement().getType();
			if ( isPrimitiveArray() ) {
				return ( (PrimitiveJavaType<?>) ( (BasicType<?>) elementType ).getJavaTypeDescriptor() ).getPrimitiveClass();
			}
			return elementType.getReturnedClass();
		}
		else {
			try {
				return getMetadata().getMetadataBuildingOptions()
						.getServiceRegistry()
						.requireService( ClassLoaderService.class )
						.classForName( elementClassName );
			}
			catch (ClassLoadingException e) {
				throw new MappingException( e );
			}
		}
	}

	@Override
	public CollectionType getDefaultCollectionType() throws MappingException {
		return new ArrayType( getRole(), getReferencedPropertyName(), getElementClass() );
	}

	@Override
	public boolean isArray() {
		return true;
	}

	/**
	 * @return Returns the elementClassName.
	 */
	public String getElementClassName() {
		return elementClassName;
	}

	/**
	 * @param elementClassName The elementClassName to set.
	 */
	public void setElementClassName(String elementClassName) {
		this.elementClassName = elementClassName;
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept( this );
	}
}
