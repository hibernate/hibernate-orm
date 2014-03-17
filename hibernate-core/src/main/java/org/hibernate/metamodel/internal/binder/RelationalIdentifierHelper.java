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
package org.hibernate.metamodel.internal.binder;

import org.hibernate.cfg.ObjectNameNormalizer;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.relational.Identifier;

/**
 * @author Gail Badner
 */
public class RelationalIdentifierHelper {

	private final BinderRootContext helperContext;

	RelationalIdentifierHelper(BinderRootContext helperContext) {
		this.helperContext = helperContext;
	}

	public String normalizeDatabaseIdentifier(
			final String explicitName,
			final ObjectNameNormalizer.NamingStrategyHelper helper) {
		return getObjectNameNormalizer().normalizeDatabaseIdentifier( explicitName, helper );
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
		return getObjectNameNormalizer().normalizeIdentifierQuoting( name );
	}

	private ObjectNameNormalizer getObjectNameNormalizer() {
		return helperContext.getMetadataCollector().getObjectNameNormalizer();
	}
}
