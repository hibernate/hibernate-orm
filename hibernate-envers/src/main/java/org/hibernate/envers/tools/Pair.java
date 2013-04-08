/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.tools;

/**
 * A pair of objects.
 * @param <T1>
 * @param <T2>
 * @author Adam Warski (adamw@aster.pl)
 */
public class Pair<T1, T2> {
    private T1 obj1;
    private T2 obj2;

    public Pair(T1 obj1, T2 obj2) {
        this.obj1 = obj1;
        this.obj2 = obj2;
    }

    public T1 getFirst() {
        return obj1;
    }

    public T2 getSecond() {
        return obj2;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;

        Pair pair = (Pair) o;

        if (obj1 != null ? !obj1.equals(pair.obj1) : pair.obj1 != null) return false;
        if (obj2 != null ? !obj2.equals(pair.obj2) : pair.obj2 != null) return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = (obj1 != null ? obj1.hashCode() : 0);
        result = 31 * result + (obj2 != null ? obj2.hashCode() : 0);
        return result;
    }

    public static <T1, T2> Pair<T1, T2> make(T1 obj1, T2 obj2) {
        return new Pair<T1, T2>(obj1, obj2);
    }
}
