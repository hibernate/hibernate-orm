package org.hibernate.test.cascade;

import java.util.Set;
import java.util.HashSet;

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 *
 * Copyright 2008 Ovidiu Feodorov
 *
 * @version <tt>$Revision$</tt>
 *
 * $Id$
 */
public class G
{
    // Constants -----------------------------------------------------------------------------------

    // Static --------------------------------------------------------------------------------------

    // Attributes ----------------------------------------------------------------------------------

    private long id;

    private String data;

    // A 1 <-> 1 G
    private A a;

    // G * <-> * H
    private Set hs;

    // Constructors --------------------------------------------------------------------------------

    public G()
    {
        this(null);
    }

    public G(String data)
    {
        this.data = data;
        hs = new HashSet();
    }

    // Public --------------------------------------------------------------------------------------

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

    public Set getHs()
    {
        return hs;
    }

    public void setHs(Set s)
    {
        hs = s;
    }

    // Package protected ---------------------------------------------------------------------------

    long getId()
    {
        return id;
    }

    // Protected -----------------------------------------------------------------------------------

    // Private -------------------------------------------------------------------------------------

    private void setId(long id)
    {
        this.id = id;
    }

    // Inner classes -------------------------------------------------------------------------------
}
