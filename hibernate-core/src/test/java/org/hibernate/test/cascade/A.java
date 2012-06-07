//$Id: $
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
 *
 */

package org.hibernate.test.cascade;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 *
 * Copyright 2008 Ovidiu Feodorov
 *
 */
public class A
{
    // Constants -----------------------------------------------------------------------------------

    // Static --------------------------------------------------------------------------------------

    // Attributes ----------------------------------------------------------------------------------

    private long id;

    private String data;

    // A 1 - * H
    private Set hs;

    // A 1 - 1 G
    private G g;


    // Constructors --------------------------------------------------------------------------------

    public A()
    {
        hs = new HashSet();
    }

    public A(String data)
    {
        this();
        this.data = data;
    }

    // Public --------------------------------------------------------------------------------------

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public String getData()
    {
        return data;
    }

    public void setHs(Set hs)
    {
        this.hs = hs;
    }

    public Set getHs()
    {
        return hs;
    }

    public void setG(G g)
    {
        this.g = g;
    }

    public G getG()
    {
        return g;
    }

    public void addH(H h)
    {
        hs.add(h);
        h.setA(this);
    }

    public String toString()
    {
        return "A[" + id + ", " + data + "]";
    }

    // Package protected ---------------------------------------------------------------------------

    // Protected -----------------------------------------------------------------------------------

    // Private -------------------------------------------------------------------------------------

    // Inner classes -------------------------------------------------------------------------------
}
