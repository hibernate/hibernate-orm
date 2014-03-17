/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.metamodel.source.internal.annotations.entity.EntityBindingContext;
import org.hibernate.metamodel.source.internal.annotations.util.JPADotNames;
import org.hibernate.metamodel.source.internal.annotations.util.JandexHelper;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

/**
 * Contains the information about a single {@link javax.persistence.AttributeOverride}. Instances of this class
 * are creating during annotation processing and then applied onto the persistence attributes.
 *
 * @author Hardy Ferentschik
 * @todo Take care of prefixes of the form 'element', 'key' and 'value'. Add another type enum to handle this. (HF)
 */
public class AttributeOverride extends AbstractOverrideDefinition{
	private final Column column;
	private final AnnotationInstance columnAnnotation;

	public AttributeOverride(
			String prefix,
			AnnotationInstance attributeOverrideAnnotation,
			EntityBindingContext bindingContext) {
		super( prefix, attributeOverrideAnnotation, bindingContext );

		this.columnAnnotation = JandexHelper.getValue(
				attributeOverrideAnnotation,
				"column",
				AnnotationInstance.class,
				bindingContext.getServiceRegistry().getService( ClassLoaderService.class )
		);
		this.column = new Column( columnAnnotation );
	}

	@Override
	protected DotName getTargetAnnotation() {
		return JPADotNames.ATTRIBUTE_OVERRIDE;
	}

	public Column getImpliedColumn() {
		return column;
	}

	public AnnotationInstance getOverriddenColumnInfo() {
		return columnAnnotation;
	}

	@Override
	public void apply(AbstractPersistentAttribute persistentAttribute) {
		int columnSize = persistentAttribute.getColumnValues().size();
		switch ( columnSize ){
			case 0:
				persistentAttribute.getColumnValues().add( column );
				break;
			case 1:
				persistentAttribute.getColumnValues().get( 0 ).applyColumnValues( columnAnnotation );
				break;
			default:
				//TODO throw exception??
		}
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( !( o instanceof AttributeOverride ) ) {
			return false;
		}
		if ( !super.equals( o ) ) {
			return false;
		}

		AttributeOverride that = (AttributeOverride) o;

		if ( column != null ? !column.equals( that.column ) : that.column != null ) {
			return false;
		}
		if ( columnAnnotation != null ? !columnAnnotation.equals( that.columnAnnotation ) : that.columnAnnotation != null ) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + ( column != null ? column.hashCode() : 0 );
		result = 31 * result + ( columnAnnotation != null ? columnAnnotation.hashCode() : 0 );
		return result;
	}
}


