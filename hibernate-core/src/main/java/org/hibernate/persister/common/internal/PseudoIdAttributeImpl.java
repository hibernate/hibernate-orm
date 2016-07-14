/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import org.hibernate.sqm.domain.ManagedType;
import org.hibernate.sqm.domain.Type;

/**
 * @author Steve Ebersole
 */
public class PseudoIdAttributeImpl extends SingularAttributeImpl {
	public PseudoIdAttributeImpl(ManagedType declaringType, Type type, Classification classification) {
		super( declaringType, "id", classification, type );
	}
}
