/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.cfg;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.FetchProfile;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Hardy Ferentschik
 */
public class VerifyFetchProfileReferenceSecondPass implements SecondPass {
	private String fetchProfileName;
	private FetchProfile.FetchOverride fetch;
	private Mappings mappings;

	public VerifyFetchProfileReferenceSecondPass(
			String fetchProfileName,
			FetchProfile.FetchOverride fetch,
			Mappings mappings) {
		this.fetchProfileName = fetchProfileName;
		this.fetch = fetch;
		this.mappings = mappings;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		org.hibernate.mapping.FetchProfile profile = mappings.findOrCreateFetchProfile(
				fetchProfileName,
				MetadataSource.ANNOTATIONS
		);
		if ( MetadataSource.ANNOTATIONS != profile.getSource() ) {
			return;
		}

		PersistentClass clazz = mappings.getClass( fetch.entity().getName() );
		// throws MappingException in case the property does not exist
		clazz.getProperty( fetch.association() );

		profile.addFetch(
				fetch.entity().getName(), fetch.association(), fetch.mode().toString().toLowerCase()
		);
	}
}


