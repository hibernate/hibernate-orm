/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.elementcollection;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.persistence.Entity;

/**
 * @author Bogdan Știrbăț
 */
@Entity
public class Homework {

    private List<String> paths;

    public List<String> getPaths() {
        return paths;
    }

    public Set<String> getPaths(String startPath) {
        TreeSet<String> result = new TreeSet<>();

        if ( paths == null ) {
            return result;
        }

        for ( String path : paths ) {
            if ( path.startsWith( startPath ) ) {
                result.add( path );
            }
        }
        return result;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public Homework setPaths(List<String> paths, boolean append) {
        if ( append ) {
            this.paths.addAll( paths );
        }
        else {
            this.paths = paths;
        }
        return this;
    }
}
