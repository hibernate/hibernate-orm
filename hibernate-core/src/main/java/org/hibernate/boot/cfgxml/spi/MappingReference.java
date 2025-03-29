/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.cfgxml.spi;

import java.io.File;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.jaxb.cfg.spi.JaxbCfgMappingReferenceType;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationException;

/**
 * Represents a {@code <mapping/>} element within a {@code cfg.xml} file.
 *
 * @author Steve Ebersole
 */
public class MappingReference {
	public enum Type {
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
			return new MappingReference( Type.CLASS, jaxbMapping.getClazz() );
		}
		else if ( StringHelper.isNotEmpty( jaxbMapping.getFile() ) ) {
			return  new MappingReference( Type.FILE, jaxbMapping.getFile() );
		}
		else if ( StringHelper.isNotEmpty( jaxbMapping.getResource() ) ) {
			return new MappingReference( Type.RESOURCE, jaxbMapping.getResource() );
		}
		else if ( StringHelper.isNotEmpty( jaxbMapping.getJar() ) ) {
			return new MappingReference( Type.JAR, jaxbMapping.getJar() );
		}
		else if ( StringHelper.isNotEmpty( jaxbMapping.getPackage() ) ) {
			return new MappingReference( Type.PACKAGE, jaxbMapping.getPackage() );
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
