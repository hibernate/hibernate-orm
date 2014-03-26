/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal.annotations.entity;

import java.util.List;
import javax.persistence.AccessType;

import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.reflite.spi.JavaTypeDescriptor;
import org.hibernate.metamodel.source.internal.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.internal.annotations.attribute.PrimaryKeyJoinColumn;
import org.hibernate.metamodel.source.internal.annotations.util.ConverterAndOverridesHelper;
import org.hibernate.metamodel.spi.AttributePath;

import org.jboss.jandex.AnnotationInstance;

import static org.hibernate.metamodel.source.internal.annotations.util.HibernateDotNames.ROW_ID;

/**
 * Representation of metadata (configured via annotations or orm.xml) attached
 * to an Entity that is the root of a persistence hierarchy.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 * @author Brett Meyer
 */
public class RootEntityTypeMetadata extends EntityTypeMetadata {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( RootEntityTypeMetadata.class );

	private final String rowId;

	public RootEntityTypeMetadata(
			JavaTypeDescriptor javaTypeDescriptor,
			AccessType defaultHierarchyAccessType,
			AnnotationBindingContext context) {
		super( javaTypeDescriptor, defaultHierarchyAccessType, context );

		// ROWID
		final AnnotationInstance rowIdAnnotation = javaTypeDescriptor.findTypeAnnotation( ROW_ID );
		this.rowId = rowIdAnnotation != null && rowIdAnnotation.value() != null
				? rowIdAnnotation.value().asString()
				: null;
	}

	@Override
	protected void collectConversionInfo() {
		super.collectConversionInfo();
	}

	@Override
	protected void collectAttributeOverrides() {
		collectAttributeOverrides( this );
	}

	private void collectAttributeOverrides(IdentifiableTypeMetadata type) {
		// subclasses first, since they have precedence
		ConverterAndOverridesHelper.INSTANCE.processAttributeOverrides(
				new AttributePath(),
				type,
				this,
				getLocalBindingContext()
		);

		if ( type.getSuperType() != null ) {
			collectAttributeOverrides( type.getSuperType() );
		}
	}

	@Override
	protected void collectAssociationOverrides() {
		super.collectAssociationOverrides();
	}

	@Override
	protected List<PrimaryKeyJoinColumn> determinePrimaryKeyJoinColumns() {
		List<PrimaryKeyJoinColumn> results = super.determinePrimaryKeyJoinColumns();
		if ( CollectionHelper.isNotEmpty( results ) ) {
			LOG.invalidPrimaryKeyJoinColumnAnnotation();
		}
		return null;
	}

	public String getRowId() {
		return rowId;
	}
}
