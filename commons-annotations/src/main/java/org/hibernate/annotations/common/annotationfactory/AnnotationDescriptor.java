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
package org.hibernate.annotations.common.annotationfactory;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates the data you need to create an annotation. In
 * particular, it stores the type of an <code>Annotation</code> instance
 * and the values of its elements.
 * The "elements" we're talking about are the annotation attributes,
 * not its targets (the term "element" is used ambiguously
 * in Java's annotations documentation).
 *
 * @author Paolo Perrotta
 * @author Davide Marchignoli
 */
public class AnnotationDescriptor {

	private final Class<? extends Annotation> type;

	private final Map<String, Object> elements = new HashMap<String, Object>();

	public AnnotationDescriptor(Class<? extends Annotation> annotationType) {
		type = annotationType;
	}

	public void setValue(String elementName, Object value) {
		elements.put( elementName, value );
	}

	public Object valueOf(String elementName) {
		return elements.get( elementName );
	}

	public boolean containsElement(String elementName) {
		return elements.containsKey( elementName );
	}

	public int numberOfElements() {
		return elements.size();
	}

	public Class<? extends Annotation> type() {
		return type;
	}
}
