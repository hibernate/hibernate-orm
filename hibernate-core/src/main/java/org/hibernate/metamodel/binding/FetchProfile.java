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
package org.hibernate.metamodel.binding;

import java.util.Collections;
import java.util.Set;

/**
 * A fetch profile allows a user to dynamically modify the fetching strategy used for particular associations at runtime, whereas
 * that information was historically only statically defined in the metadata.
 * <p/>
 * This class represent the data as it is defined in their metadata.
 *
 * @author Steve Ebersole
 * @see org.hibernate.engine.profile.FetchProfile
 */
public class FetchProfile {

    private final String name;
    private final Set<Fetch> fetches;

    /**
     * Create a fetch profile representation.
     *
     * @param name The name of the fetch profile.
     * @param fetches
     */
    public FetchProfile( String name,
                         Set<Fetch> fetches ) {
        this.name = name;
        this.fetches = fetches;
    }

    /**
     * Retrieve the name of the fetch profile.
     *
     * @return The profile name
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieve the fetches associated with this profile
     *
     * @return The fetches associated with this profile.
     */
    public Set<Fetch> getFetches() {
        return Collections.unmodifiableSet(fetches);
    }

    /**
     * Adds a fetch to this profile.
     *
     * @param entity The entity which contains the association to be fetched
     * @param association The association to fetch
     * @param style The style of fetch t apply
     */
    public void addFetch( String entity,
                          String association,
                          String style ) {
        fetches.add(new Fetch(entity, association, style));
    }

    /**
     * Defines an individual association fetch within the given profile.
     */
    public static class Fetch {
        private final String entity;
        private final String association;
        private final String style;

        public Fetch( String entity,
                      String association,
                      String style ) {
            this.entity = entity;
            this.association = association;
            this.style = style;
        }

        public String getEntity() {
            return entity;
        }

        public String getAssociation() {
            return association;
        }

        public String getStyle() {
            return style;
        }
    }
}
