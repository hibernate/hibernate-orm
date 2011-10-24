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
package org.hibernate.id;
import java.io.Serializable;
import java.util.UUID;

import org.hibernate.engine.spi.SessionImplementor;

/**
 * A strategy for generating a variant 2 {@link UUID} value.
 *
 * @author Steve Ebersole
 */
public interface UUIDGenerationStrategy extends Serializable {
	/**
	 * Which variant, according to IETF RFC 4122, of UUID does this strategy generate?  RFC 4122 defines
	 * 5 variants (though it only describes algorithms to generate 4):<ul>
	 * <li>1 = time based</li>
	 * <li>2 = DCE based using POSIX UIDs</li>
	 * <li>3 = name based (md5 hash)</li>
	 * <li>4 = random numbers based</li>
	 * <li>5 = name based (sha-1 hash)</li>
	 * </ul>
	 * Returning the values above should be reserved to those generators creating variants compliant with the
	 * corresponding RFC definition; others can feel free to return other values as they see fit.
	 * <p/>
	 * Informational only, and not used at this time.
	 *
	 * @return The supported generation version
	 */
	public int getGeneratedVersion();

	/**
	 * Generate the UUID.
	 *
	 * @param session The session asking for the generation
	 *
	 * @return The generated UUID.
	 */
	public UUID generateUUID(SessionImplementor session);

}
