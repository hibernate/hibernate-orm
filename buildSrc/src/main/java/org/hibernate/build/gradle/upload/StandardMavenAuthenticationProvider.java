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
package org.hibernate.build.gradle.upload;

import org.apache.maven.artifact.ant.Authentication;
import org.apache.maven.artifact.ant.RemoteRepository;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider of {@link org.apache.maven.artifact.ant.RemoteRepository} {@link Authentication} based on standard Maven
 * conventions using {@literal settings.xml}.
 *
 * @author Steve Ebersole
 */
public class StandardMavenAuthenticationProvider implements AuthenticationProvider {
	private static final Logger log = LoggerFactory.getLogger( StandardMavenAuthenticationProvider.class );

	public static final String SETTINGS_LOCATION_OVERRIDE = "maven.settings";

	private ConcurrentHashMap<String,Authentication> repositoryAuthenticationMap;

	@Override
	public Authentication determineAuthentication(RemoteRepository remoteRepository) {
		if ( repositoryAuthenticationMap == null ) {
			loadRepositoryAuthenticationMap();
		}

		return repositoryAuthenticationMap.get( remoteRepository.getId() );
	}

	private void loadRepositoryAuthenticationMap() {
		repositoryAuthenticationMap = new ConcurrentHashMap<String, Authentication>();

		final File settingsFile = determineSettingsFileLocation();
		try {
			InputSource inputSource = new InputSource( new FileInputStream( settingsFile ) );
			try {
				final Document document = buildSAXReader().read( inputSource );
				final Element settingsElement = document.getRootElement();
				final Element serversElement = settingsElement.element( "servers" );
				final Iterator serversIterator = serversElement.elementIterator( "server" );
				while ( serversIterator.hasNext() ) {
					final Element serverElement = (Element) serversIterator.next();
					final String id = extractValue( serverElement.element( "id" ) );
					if ( id == null ) {
						continue;
					}
					final Authentication authentication = new Authentication();
					authentication.setUserName( extractValue( serverElement.element( "username" ) ) );
					authentication.setPassword( extractValue( serverElement.element( "password" ) ) );
					authentication.setPrivateKey( extractValue( serverElement.element( "privateKey" ) ) );
					authentication.setPassphrase( extractValue( serverElement.element( "passphrase" ) ) );
					repositoryAuthenticationMap.put( id, authentication );
				}
			}
			catch (DocumentException e) {
				log.error( "Error reading Maven settings.xml", e );
			}
		}
		catch ( FileNotFoundException e ) {
			log.info( "Unable to locate Maven settings.xml" );
		}
	}

	private String extractValue(Element element) {
		if ( element == null ) {
			return null;
		}

		final String value = element.getTextTrim();
		if ( value != null && value.length() == 0 ) {
			return null;
		}

		return value;
	}

	private SAXReader buildSAXReader() {
		SAXReader saxReader = new SAXReader();
		saxReader.setMergeAdjacentText( true );
		return saxReader;
	}

	private File determineSettingsFileLocation() {
		final String overrideLocation = System.getProperty( SETTINGS_LOCATION_OVERRIDE );
		return overrideLocation == null
				? new File( new File( System.getProperty( "user.home" ), ".m2" ), "settings.xml" )
				: new File( overrideLocation );
	}
}
