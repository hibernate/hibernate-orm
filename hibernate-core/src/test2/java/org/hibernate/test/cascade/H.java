/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: $


package org.hibernate.test.cascade;
import java.util.HashSet;
import java.util.Set;

public class H
{
    // Constants -----------------------------------------------------------------------------------

    // Static --------------------------------------------------------------------------------------

    // Attributes ----------------------------------------------------------------------------------

    private long id;

    private String data;

    private A a;

    // G * <-> * H
    private Set gs;

    // Constructors --------------------------------------------------------------------------------

    public H()
    {
        this(null);
    }

    public H(String data)
    {
        this.data = data;
        gs = new HashSet();
    }

    // Public --------------------------------------------------------------------------------------

    public long getId()
    {
        return id;
    }

    public String getData()
    {
        return data;
    }

    public void setData(String data)
    {
        this.data = data;
    }

    public A getA()
    {
        return a;
    }

    public void setA(A a)
    {
        this.a = a;
    }

    public Set getGs()
    {
        return gs;
    }

    public void setGs(Set gs)
    {
        this.gs = gs;
    }

    // Package protected ---------------------------------------------------------------------------

    // Protected -----------------------------------------------------------------------------------

    // Private -------------------------------------------------------------------------------------

    private void setId(long id)
    {
        this.id = id;
    }

    // Inner classes -------------------------------------------------------------------------------
}
