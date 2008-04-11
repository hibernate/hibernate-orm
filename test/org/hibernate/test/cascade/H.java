// $Id$


package org.hibernate.test.cascade;

import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 *
 * Copyright 2008 Ovidiu Feodorov
 *
 */
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
