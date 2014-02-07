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
package org.hibernate.metamodel.internal.source.hbm.transform;

import java.util.Date;

import org.hibernate.jaxb.spi.hbm.JaxbFilterDefElement;
import org.hibernate.jaxb.spi.hbm.JaxbFilterParamElement;
import org.hibernate.jaxb.spi.hbm.JaxbHibernateMapping;
import org.hibernate.jaxb.spi.hbm.JaxbIdentifierGeneratorElement;
import org.hibernate.jaxb.spi.hbm.JaxbImportElement;
import org.hibernate.jaxb.spi.hbm.JaxbMetaContainerElement;
import org.hibernate.jaxb.spi.hbm.JaxbMetaElement;
import org.hibernate.jaxb.spi.hbm.JaxbParamElement;
import org.hibernate.jaxb.spi.hbm.JaxbTypedefElement;
import org.hibernate.metamodel.spi.source.jaxb.JaxbEntityMappings;
import org.hibernate.metamodel.spi.source.jaxb.JaxbHbmFilterDef;
import org.hibernate.metamodel.spi.source.jaxb.JaxbHbmIdGeneratorDef;
import org.hibernate.metamodel.spi.source.jaxb.JaxbHbmParam;
import org.hibernate.metamodel.spi.source.jaxb.JaxbHbmToolingHint;
import org.hibernate.metamodel.spi.source.jaxb.JaxbHbmTypeDef;
import org.hibernate.metamodel.spi.source.jaxb.JaxbPersistenceUnitMetadata;

/**
 * Transforms a JAXB binding of a hbm.xml file into a unified orm.xml representation
 *
 * @author Steve Ebersole
 */
public class HbmXmlTransformer {
	/**
	 * Singleton access
	 */
	public static final HbmXmlTransformer INSTANCE = new HbmXmlTransformer();

	public JaxbEntityMappings transform(JaxbHibernateMapping hbmXmlMapping) {
		final JaxbEntityMappings ormRoot = new JaxbEntityMappings();
		ormRoot.setDescription(
				"Hibernate orm.xml document auto-generated from legacy hbm.xml format via transformation " +
						"(generated at " +  new Date().toString() + ")"
		);

		final JaxbPersistenceUnitMetadata metadata = new JaxbPersistenceUnitMetadata();
		ormRoot.setPersistenceUnitMetadata( metadata );
		metadata.setDescription(
				"Defines information which applies to the persistence unit overall, not just to this mapping file.\n\n" +
						"This transformation only specifies xml-mapping-metadata-complete."
		);

		transferToolingHints( hbmXmlMapping, ormRoot );

		ormRoot.setPackage( hbmXmlMapping.getPackage() );
		ormRoot.setSchema( hbmXmlMapping.getSchema() );
		ormRoot.setCatalog( hbmXmlMapping.getCatalog() );
		ormRoot.setCustomAccess( hbmXmlMapping.getDefaultAccess() );
		ormRoot.setDefaultCascade( hbmXmlMapping.getDefaultCascade() );
		ormRoot.setAutoImport( hbmXmlMapping.isAutoImport() );
		ormRoot.setDefaultLazy( hbmXmlMapping.isDefaultLazy() );

		transferTypeDefs( hbmXmlMapping, ormRoot );
		transferFilterDefs( hbmXmlMapping, ormRoot );
		transferIdentifierGenerators( hbmXmlMapping, ormRoot );
		transferImports( hbmXmlMapping, ormRoot );


//		<xs:element name="resultset" type="resultset-element"/>
//		<xs:group ref="query-or-sql-query"/>
//		<xs:element name="fetch-profile" type="fetch-profile-element"/>
//		<xs:element name="database-object" type="database-object-element"/>

//		<xs:element name="class" type="class-element"/>
//		<xs:element name="subclass" type="subclass-element"/>
//		<xs:element name="joined-subclass" type="joined-subclass-element"/>
//		<xs:element name="union-subclass" type="union-subclass-element"/>

		return ormRoot;
	}

	private void transferToolingHints(JaxbMetaContainerElement hbmMetaContainer, JaxbEntityMappings entityMappings) {
		if ( hbmMetaContainer.getMeta().isEmpty() ) {
			return;
		}

		for ( JaxbMetaElement hbmMetaElement : hbmMetaContainer.getMeta() ) {
			final JaxbHbmToolingHint toolingHint = new JaxbHbmToolingHint();
			entityMappings.getToolingHint().add( toolingHint );
			toolingHint.setName( hbmMetaElement.getName() );
			toolingHint.setInheritable( hbmMetaElement.isInheritable() );
			toolingHint.setValue( hbmMetaElement.getValue() );
		}
	}

	private void transferTypeDefs(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getTypedef().isEmpty() ) {
			return;
		}

		for ( JaxbTypedefElement hbmXmlTypeDef : hbmXmlMapping.getTypedef() ) {
			final JaxbHbmTypeDef typeDef = new JaxbHbmTypeDef();
			ormRoot.getTypeDef().add( typeDef );
			typeDef.setName( hbmXmlTypeDef.getName() );
			typeDef.setClazz( hbmXmlTypeDef.getClazz() );

			if ( !hbmXmlTypeDef.getParam().isEmpty() ) {
				for ( JaxbParamElement hbmParam : hbmXmlTypeDef.getParam() ) {
					final JaxbHbmParam param = new JaxbHbmParam();
					typeDef.getParam().add( param );
					param.setName( hbmParam.getName() );
					param.setValue( hbmParam.getValue() );
				}
			}
		}
	}

	private void transferFilterDefs(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getFilterDef().isEmpty() ) {
			return;
		}

		for ( JaxbFilterDefElement hbmFilterDef : hbmXmlMapping.getFilterDef() ) {
			JaxbHbmFilterDef filterDef = new JaxbHbmFilterDef();
			ormRoot.getFilterDef().add( filterDef );
			filterDef.setName( hbmFilterDef.getName() );

			boolean foundCondition = false;
			for ( Object content : hbmFilterDef.getContent() ) {
				if ( String.class.isInstance( content ) ) {
					foundCondition = true;
					filterDef.setCondition( (String) content );
				}
				else {
					JaxbFilterParamElement hbmFilterParam = (JaxbFilterParamElement) content;
					JaxbHbmFilterDef.JaxbFilterParam param = new JaxbHbmFilterDef.JaxbFilterParam();
					filterDef.getFilterParam().add( param );
					param.setName( hbmFilterParam.getParameterName() );
					param.setType( hbmFilterParam.getParameterValueTypeName() );
				}
			}

			if ( !foundCondition ) {
				filterDef.setCondition( hbmFilterDef.getCondition() );
			}
		}
	}

	private void transferIdentifierGenerators(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getIdentifierGenerator().isEmpty() ) {
			return;
		}

		for ( JaxbIdentifierGeneratorElement hbmGenerator : hbmXmlMapping.getIdentifierGenerator() ) {
			final JaxbHbmIdGeneratorDef generatorDef = new JaxbHbmIdGeneratorDef();
			ormRoot.getIdentifierGeneratorDef().add( generatorDef );
			generatorDef.setName( hbmGenerator.getName() );
			generatorDef.setClazz( hbmGenerator.getClazz() );
		}
	}

	private void transferImports(JaxbHibernateMapping hbmXmlMapping, JaxbEntityMappings ormRoot) {
		if ( hbmXmlMapping.getImport().isEmpty() ) {
			return;
		}

		for ( JaxbImportElement hbmImport : hbmXmlMapping.getImport() ) {
			final JaxbEntityMappings.JaxbImport ormImport = new JaxbEntityMappings.JaxbImport();
			ormRoot.getImport().add( ormImport );
			ormImport.setClazz( hbmImport.getClazz() );
			ormImport.setRename( hbmImport.getRename() );
		}
	}

}
