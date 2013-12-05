/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
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
package org.hibernate.loader.plan.spi;

/**
 * Represents the circular side of a bi-directional entity association. Wraps a reference to the associated
 * (target) EntityReference.
 * <p/>
 * The {@link org.hibernate.loader.plan.exec.spi.EntityReferenceAliases} for this object is the same as
 * for its target EntityReference, and can be looked up via
 * {@link org.hibernate.loader.plan.exec.spi.AliasResolutionContext#resolveEntityReferenceAliases(String)}
 * using the value returned by {@link #getQuerySpaceUid()}.
 *
 * This relies on reference lookups against the EntityReference instances, therefore this allows representation of the
 * circularity but with a little protection against potential stack overflows.  This is unfortunately still a cyclic
 * graph.  An alternative approach is to make the graph acyclic (DAG) would be to follow the process I adopted in the
 * original HQL Antlr v3 work with regard to always applying an alias to the "persister reference", even where that
 * meant creating a generated, unique identifier as the alias.  That allows other parts of the tree to refer to the
 * "persister reference" by that alias without the need for potentially cyclic graphs (think ALIAS_REF in the current
 * ORM parser).  Those aliases can then be mapped/catalogued against the "persister reference" for retrieval as needed.
 *
 * @author Steve Ebersole
 */
public interface BidirectionalEntityReference extends EntityReference {
	/**
	 * Get the targeted EntityReference
	 *
	 * @return The targeted EntityReference
	 */
	public EntityReference getTargetEntityReference();

	/**
	 * The query space UID returned using {@link #getQuerySpaceUid()} must
	 * be the same as returned by {@link #getTargetEntityReference()#getQuerySpaceUid()}
	 *
	 * @return The query space UID.
	 */
	public String getQuerySpaceUid();
}
