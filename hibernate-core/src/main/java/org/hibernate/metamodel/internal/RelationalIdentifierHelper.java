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
package org.hibernate.metamodel.internal;

import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.source.LocalBindingContext;

/**
 * @author Gail Badner
 */
public class RelationalIdentifierHelper {

	private final LocalBindingContext bindingContext;

	RelationalIdentifierHelper(LocalBindingContext bindingContext) {
		this.bindingContext = bindingContext;
	}

	public String normalizeDatabaseIdentifier(
			final String explicitName,
			ObjectNameNormalizer.NamingStrategyHelper helper) {
		return bindingContext
				.getMetadataImplementor()
				.getObjectNameNormalizer()
				.normalizeDatabaseIdentifier( explicitName, helper );
	}

	public Identifier createIdentifier(final String name){
		return createIdentifier( name, null );
	}

	public Identifier createIdentifier(final String name, final String defaultName) {
		String identifier = StringHelper.isEmpty( name ) ? defaultName : name;
		identifier = quotedIdentifier( identifier );
		return Identifier.toIdentifier( identifier );
	}

	public String quotedIdentifier(final String name) {
		return bindingContext.getMetadataImplementor().getObjectNameNormalizer().normalizeIdentifierQuoting( name );
	}

}
