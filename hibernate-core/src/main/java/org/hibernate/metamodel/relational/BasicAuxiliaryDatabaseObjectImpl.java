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
package org.hibernate.metamodel.relational;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.Metadata;
import org.hibernate.metamodel.source.spi.MetadataImplementor;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class BasicAuxiliaryDatabaseObjectImpl extends AbstractAuxiliaryDatabaseObject {
	private static final String CATALOG_NAME_PLACEHOLDER = "${catalog}";
	private static final String SCHEMA_NAME_PLACEHOLDER = "${schema}";
	private final String createString;
	private final String dropString;

	public BasicAuxiliaryDatabaseObjectImpl(String createString, String dropString, Set<String> dialectScopes) {
		super( dialectScopes );
		this.createString = createString;
		this.dropString = dropString;
	}

	@Override
	public String[] sqlCreateStrings(MetadataImplementor metadata) {
		return new String[] { injectCatalogAndSchema( createString, metadata.getOptions() ) };
	}

	@Override
	public String[] sqlDropStrings(MetadataImplementor metadata) {
		return new String[] { injectCatalogAndSchema( dropString, metadata.getOptions() ) };
	}

	private String injectCatalogAndSchema(String ddlString, Metadata.Options options) {
		String rtn = StringHelper.replace( ddlString, CATALOG_NAME_PLACEHOLDER, options.getDefaultCatalogName() );
		rtn = StringHelper.replace( rtn, SCHEMA_NAME_PLACEHOLDER, options.getDefaultSchemaName() );
		return rtn;
	}

}
