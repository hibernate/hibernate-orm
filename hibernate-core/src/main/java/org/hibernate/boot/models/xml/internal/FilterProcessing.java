/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.xml.internal;

import java.util.List;

import org.hibernate.annotations.SqlFragmentAlias;
import org.hibernate.boot.jaxb.mapping.spi.JaxbFilterImpl;
import org.hibernate.boot.models.HibernateAnnotations;
import org.hibernate.boot.models.annotations.internal.SqlFragmentAliasAnnotation;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.ClassDetails;

/**
 * @author Steve Ebersole
 */
public class FilterProcessing {
	private static final SqlFragmentAlias[] NO_ALIASES = new SqlFragmentAlias[0];

	public static SqlFragmentAlias[] collectSqlFragmentAliases(
			List<JaxbFilterImpl.JaxbAliasesImpl> jaxbAliases,
			XmlDocumentContext xmlDocumentContext) {
		if ( CollectionHelper.isEmpty( jaxbAliases ) ) {
			return NO_ALIASES;
		}

		final SqlFragmentAlias[] result = new SqlFragmentAlias[jaxbAliases.size()];
		for ( int i = 0; i < jaxbAliases.size(); i++ ) {
			final SqlFragmentAliasAnnotation alias = HibernateAnnotations.SQL_FRAGMENT_ALIAS.createUsage(
					xmlDocumentContext.getModelBuildingContext()
			);
			result[i] = alias;

			final JaxbFilterImpl.JaxbAliasesImpl jaxbAlias = jaxbAliases.get( i );
			alias.alias( jaxbAlias.getAlias() );
			if ( StringHelper.isNotEmpty( jaxbAlias.getTable() ) ) {
				alias.table( jaxbAlias.getTable() );
			}
			if ( StringHelper.isNotEmpty( jaxbAlias.getEntity() ) ) {
				final ClassDetails classDetails = xmlDocumentContext.getModelBuildingContext()
						.getClassDetailsRegistry()
						.resolveClassDetails( jaxbAlias.getEntity() );
				alias.entity( classDetails.toJavaClass() );
			}
		}
		return result;
	}
}
