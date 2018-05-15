/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import java.util.Map;

/**
 * @author Chris Cranford
 */
public interface TypeResolverTemplate {
	/**
	 * Resolve the TypeResolver based on (1) the TypeDef that
	 * this template represents and (2) config parameters objects
	 * passed in.  The "local config parameters" come from the
	 * {@code @Type} that refers to this TypeResolverTemplate.
	 *
	 * Note that the TypeDef also defines config parameters.  These
	 * need to be combined with the incoming ones (the local ones
	 * winning).
	 *
	 * Note also that if no config parameters are passed in it is
	 * conceivable that this TypeResolverTemplate could cache and
	 * re-use a TypeResolver instance specific to the TypeDef config.
	 *
	 * @see org.hibernate.boot.model.TypeDefinition
	 * @see org.hibernate.annotations.TypeDef#parameters()
	 * @see org.hibernate.annotations.Type#parameters()
	 */
	BasicTypeResolver resolveTypeResolver(Map<String, String> localConfigParameters);
}
