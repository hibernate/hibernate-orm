/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc..
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
package org.hibernate.metamodel.source.internal.jandex;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.source.internal.jaxb.PersistentAttribute;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;

/**
 * @author Brett Meyer
 */
public class NaturalIdMocker extends PropertyMocker {
	private final PersistentAttribute attribute;
	private final boolean mutable;

	NaturalIdMocker(IndexBuilder indexBuilder, ClassInfo classInfo, Default defaults,
			PersistentAttribute attribute, boolean mutable) {
		super( indexBuilder, classInfo, defaults );
		this.attribute = attribute;
		this.mutable = mutable;
	}

	@Override
	protected void doProcess() {
		List<AnnotationValue> annotationValueList = new ArrayList<AnnotationValue>();
		MockHelper.booleanValue( "mutable", mutable, annotationValueList );
		create( NATURAL_ID, annotationValueList );
	}

	@Override
	protected PersistentAttribute getPersistentAttribute() {
		return attribute;
	}
}
