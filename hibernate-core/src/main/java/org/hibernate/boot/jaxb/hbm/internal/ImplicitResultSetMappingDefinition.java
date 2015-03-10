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
package org.hibernate.boot.jaxb.hbm.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryCollectionLoadReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryJoinReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryReturnType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmNativeQueryScalarReturnType;
import org.hibernate.boot.jaxb.hbm.spi.ResultSetMappingBindingDefinition;
import org.hibernate.internal.util.collections.CollectionHelper;

/**
 * Models the ResultSet mapping defined inline as part of a named native query definition
 *
 * @author Steve Ebersole
 */
public class ImplicitResultSetMappingDefinition implements ResultSetMappingBindingDefinition {
	private final String name;
	private final List valueMappingSources;

	public ImplicitResultSetMappingDefinition(
			String resultSetMappingName,
			List valueMappingSources) {
		this.name = resultSetMappingName;
		this.valueMappingSources = valueMappingSources;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List getValueMappingSources() {
		return valueMappingSources;
	}

	public static class Builder {
		private final String queryName;

		private List valueMappingSources;

		public Builder(String queryName) {
			this.queryName = queryName;
		}

		public void addReturn(JaxbHbmNativeQueryScalarReturnType scalarReturn) {
			if ( valueMappingSources == null ) {
				valueMappingSources = new ArrayList();
			}
			valueMappingSources.add( scalarReturn );
		}

		public void addReturn(JaxbHbmNativeQueryReturnType rootReturn) {
			if ( valueMappingSources == null ) {
				valueMappingSources = new ArrayList();
			}
			valueMappingSources.add( rootReturn );
		}

		public void addReturn(JaxbHbmNativeQueryJoinReturnType joinReturn) {
			if ( valueMappingSources == null ) {
				valueMappingSources = new ArrayList();
			}
			valueMappingSources.add( joinReturn );
		}

		public void addReturn(JaxbHbmNativeQueryCollectionLoadReturnType collectionLoadReturn) {
			if ( valueMappingSources == null ) {
				valueMappingSources = new ArrayList<JaxbHbmNativeQueryCollectionLoadReturnType>();
			}
			valueMappingSources.add( collectionLoadReturn );
		}

		public boolean hasAnyReturns() {
			return CollectionHelper.isNotEmpty( valueMappingSources );
		}

		public ImplicitResultSetMappingDefinition build() {
			return new ImplicitResultSetMappingDefinition(
					queryName + "-inline-result-set-mapping-def",
					copy( valueMappingSources )
			);
		}

		private <T> List<T> copy(List<T> returnBindings) {
			if ( returnBindings == null ) {
				return Collections.emptyList();
			}

			return Collections.unmodifiableList( returnBindings );
		}
	}
}
