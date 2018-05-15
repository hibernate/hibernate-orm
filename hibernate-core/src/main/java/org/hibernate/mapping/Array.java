/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.collection.internal.StandardArraySemantics;
import org.hibernate.collection.spi.CollectionSemantics;
import org.hibernate.type.descriptor.java.spi.Primitive;

/**
 * An array mapping has a primary key consisting of the key columns + index column.
 *
 * @author Gavin King
 */
public class Array extends List {
	private String elementClassName;
	private final CollectionJavaTypeMapping javaTypeMapping;

	public Array(MetadataBuildingContext buildingContext, PersistentClass owner) {
		super( buildingContext, owner );

		javaTypeMapping = new CollectionJavaTypeMapping(
				buildingContext.getBootstrapContext().getTypeConfiguration(),
				Object[].class
		);

	}

	public Class getElementClass() throws MappingException {
		if ( elementClassName == null ) {
			return isPrimitiveArray()
					? ( (Primitive) getElement().getJavaTypeMapping().getJavaTypeDescriptor() ).getPrimitiveClass()
					: getElement().getJavaTypeMapping().getJavaTypeDescriptor().getJavaType();
		}
		else {
			try {
				return getMetadataBuildingContext()
						.getBuildingOptions()
						.getServiceRegistry()
						.getService( ClassLoaderService.class )
						.classForName( elementClassName );
			}
			catch (ClassLoadingException e) {
				throw new MappingException( e );
			}
		}
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

	@Override
	@SuppressWarnings("unchecked")
	public CollectionSemantics getCollectionSemantics() {
		return StandardArraySemantics.INSTANCE;
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}
}
