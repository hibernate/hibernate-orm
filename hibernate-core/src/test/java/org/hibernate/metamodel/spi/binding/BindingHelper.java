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
package org.hibernate.metamodel.spi.binding;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class BindingHelper {
	/**
	 * Help to get an attribute binding that we are fully expecting to exist.
	 * <p/>
	 * Helpful because it validates that the attribute exists and manages checking the
	 * specific type and casting.
	 *
	 * @param attributeContainer The container for the attribute
	 * @param attributeName The name of the attribute to get
	 * @param expectedType The specific AttributeBinding sub-type we are expecting
	 * @param <T> The generic representation of `expectedType`
	 *
	 * @return The typed attribute binding
	 */
	public static <T extends AttributeBinding> T locateAttributeBinding(
			AttributeBindingContainer attributeContainer,
			String attributeName,
			Class<T> expectedType) {
		AttributeBinding attributeBinding = attributeContainer.locateAttributeBinding( attributeName );
		assertNotNull( "Could not locate attribute named " + attributeName, attributeBinding );
		return assertTyping( expectedType, attributeBinding );

	}
}
