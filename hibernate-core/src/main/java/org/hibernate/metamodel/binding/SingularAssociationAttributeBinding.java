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
package org.hibernate.metamodel.binding;

/**
 * Contract describing the attribute binding for singular associations ({@code many-to-one}, {@code one-to-one}).
 *
 * @author Gail Badner
 * @author Steve Ebersole
 */
@SuppressWarnings( {"JavaDoc", "UnusedDeclaration"})
public interface SingularAssociationAttributeBinding extends SingularAttributeBinding, AssociationAttributeBinding {
	/**
	 * Is this association based on a property reference (non PK column(s) as target of FK)?
	 * <p/>
	 * Convenience form of checking {@link #getReferencedAttributeName()} for {@code null}.
	 * 
	 * @return
	 */
	public boolean isPropertyReference();

	/**
	 * Obtain the name of the referenced entity.
	 *
	 * @return The referenced entity name
	 */
	public String getReferencedEntityName();

	/**
	 * Set the name of the
	 * @param referencedEntityName
	 */
	public void setReferencedEntityName(String referencedEntityName);

	public String getReferencedAttributeName();
	public void setReferencedAttributeName(String referencedAttributeName);


	// "resolvable"
	public void resolveReference(AttributeBinding attributeBinding);
	public boolean isReferenceResolved();
	public EntityBinding getReferencedEntityBinding();
	public AttributeBinding getReferencedAttributeBinding();
}