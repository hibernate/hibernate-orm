/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmIdentifierGeneratorDefinitionType;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class IdentifierGeneratorDefinitionBinder {
	private static final Logger log = Logger.getLogger( IdentifierGeneratorDefinitionBinder.class );

	public static void processIdentifierGeneratorDefinition(
			HbmLocalMetadataBuildingContext context,
			JaxbHbmIdentifierGeneratorDefinitionType identifierGenerator) {
		log.debugf( "Processing <identifier-generator/> : %s", identifierGenerator.getName() );

		context.getMetadataCollector().addIdentifierGenerator(
				new IdentifierGeneratorDefinition(
						identifierGenerator.getName(),
						identifierGenerator.getClazz()
				)
		);
	}
}
