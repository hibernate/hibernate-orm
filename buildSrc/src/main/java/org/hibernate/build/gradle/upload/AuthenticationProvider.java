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

/**
 * Contract for providers of {@link Authentication} details for authenticating against remote repositories.
 *
 * @author Steve Ebersole
 */
public interface AuthenticationProvider {
	/**
	 * The contract method.  Given a repository, determine the {@link Authentication} according to this provider's
	 * contract.  Return {@literal null} to indicate no {@link Authentication} applied for this repository by this
	 * provider.
	 *
	 * @param remoteRepository The repository to check for authentication details.
	 *
	 * @return The authentication details, or {@literal null} to indicate none.
	 */
	public Authentication determineAuthentication(RemoteRepository remoteRepository);
}
