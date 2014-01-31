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

import java.lang.reflect.Member;

import org.hibernate.jpa.internal.metamodel.AbstractManagedType;
import org.hibernate.metamodel.spi.binding.AttributeBinding;

/**
 * Contract for how we resolve the {@link java.lang.reflect.Member} for a given attribute context.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public interface MemberResolver {
	/**
	 * Resolve the member.
	 *
	 * @param owner The JPA Metamodel representation of the class tha owns the member to be resolved
	 * @param attributeBinding Hibernate metamodel representation of the attribute for which to resolve the member.
	 *
	 * @return The resolved member.
	 */
	public Member resolveMember(AbstractManagedType owner, AttributeBinding attributeBinding);
}
