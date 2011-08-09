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

package org.hibernate.metamodel.source.annotations.attribute.type;

import org.jboss.jandex.AnnotationInstance;

import org.hibernate.MappingException;
import org.hibernate.annotations.SourceType;
import org.hibernate.metamodel.source.annotations.HibernateDotNames;
import org.hibernate.metamodel.source.annotations.JandexHelper;
import org.hibernate.metamodel.source.annotations.attribute.BasicAttribute;

/**
 * Versioned Timestamp Type with {@code org.hibernate.annotations.Source}
 *
 * @author Strong Liu
 */
public class TimestampSourceTypeResolver extends AbstractAttributeTypeResolver {
    private final BasicAttribute mappedAttribute;

    public TimestampSourceTypeResolver(BasicAttribute mappedAttribute) {
        this.mappedAttribute = mappedAttribute;
    }

    @Override
    protected AnnotationInstance getTypeDeterminingAnnotationInstance() {

        AnnotationInstance annotationInstance= JandexHelper.getSingleAnnotation(
                mappedAttribute.annotations(),
                HibernateDotNames.SOURCE
        );
        if ( annotationInstance!=null && !mappedAttribute.isVersioned() ) {
            throw new MappingException( "Entity is annotated with @Source and should hence be versioned" );
        }
        return annotationInstance;
    }

    @Override
    protected String resolveHibernateTypeName(AnnotationInstance annotationInstance) {
        if ( annotationInstance == null ) {
            return null;
        }
        SourceType sourceType = JandexHelper.getEnumValue( annotationInstance, "value", SourceType.class );
        return sourceType.typeName();
    }
}
