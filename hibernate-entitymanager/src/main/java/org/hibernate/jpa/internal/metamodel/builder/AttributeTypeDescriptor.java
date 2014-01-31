/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.jpa.internal.metamodel.builder;

/**
 * Centralized access to a variety of information about a the type of an attribute being built for the JPA metamodel.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public interface AttributeTypeDescriptor {
	/**
	 * Enum of the simplified types a value might be.  These relate more to the Hibernate classification
	 * then the JPA classification
	 */
	enum ValueClassification {
		EMBEDDABLE,
		ENTITY,
		BASIC
	}

	public org.hibernate.type.Type getHibernateType();

	public Class getBindableType();

	/**
	 * Retrieve the simplified value classification
	 *
	 * @return The value type
	 */
	public ValueClassification getValueClassification();

	/**
	 * Retrieve the metadata about the attribute from which this value comes
	 *
	 * @return The "containing" attribute metadata.
	 */
	public AttributeMetadata getAttributeMetadata();
}
