/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
package org.hibernate.boot.cfgxml.spi;

import java.io.File;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgMappingReferenceType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationException;

/**
 * Represents a {@code <mapping/>} element within a cfg.xml file.
 *
 * @author Steve Ebersole
 */
public class MappingReference {
	public static enum Type {
		RESOURCE,
		CLASS,
		FILE,
		JAR,
		PACKAGE
	}

	private final Type type;
	private final String reference;

	public MappingReference(Type type, String reference) {
		this.type = type;
		this.reference = reference;
	}

	public Type getType() {
		return type;
	}

	public String getReference() {
		return reference;
	}

	public static MappingReference consume(JaxbCfgMappingReferenceType jaxbMapping) {
		if ( StringHelper.isNotEmpty( jaxbMapping.getClazz() ) ) {
			return new MappingReference( MappingReference.Type.CLASS, jaxbMapping.getClazz() );
		}
		else if ( StringHelper.isNotEmpty( jaxbMapping.getFile() ) ) {
			return  new MappingReference( MappingReference.Type.FILE, jaxbMapping.getFile() );
		}
		else if ( StringHelper.isNotEmpty( jaxbMapping.getResource() ) ) {
			return new MappingReference( MappingReference.Type.RESOURCE, jaxbMapping.getResource() );
		}
		else if ( StringHelper.isNotEmpty( jaxbMapping.getJar() ) ) {
			return new MappingReference( MappingReference.Type.JAR, jaxbMapping.getJar() );
		}
		else if ( StringHelper.isNotEmpty( jaxbMapping.getPackage() ) ) {
			return new MappingReference( MappingReference.Type.PACKAGE, jaxbMapping.getPackage() );
		}
		else {
			throw new ConfigurationException( "<mapping/> named unexpected reference type" );
		}
	}

	public void apply(MetadataSources metadataSources) {
		switch ( getType() ) {
			case RESOURCE: {
				metadataSources.addResource( getReference() );
				break;
			}
			case CLASS: {
				metadataSources.addAnnotatedClassName( getReference() );
				break;
			}
			case FILE: {
				metadataSources.addFile( getReference() );
				break;
			}
			case PACKAGE: {
				metadataSources.addPackage( getReference() );
				break;
			}
			case JAR: {
				metadataSources.addJar( new File( getReference() ) );
				break;
			}
		}
	}
}
