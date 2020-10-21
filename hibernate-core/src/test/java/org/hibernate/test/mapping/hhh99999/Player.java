package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "\"Players\"")
public class Player implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column
    private Integer id;

    @Basic(optional = false)
    @Column
    private String name;

    @OneToMany(mappedBy = "player")
    private List<TeamMember> teamMembers;

    public Player()
    {
    }

    public Player(Player p)
    {
        this(p.getId(), p.getName());
    }

    public Player(Integer id)
    {
        this(id, null);
    }

    public Player(String name)
    {
        this(null, name);
    }

    public Player(Integer id, String name)
    {
        this.id = Objects.requireNonNull(id);
        this.name = name;
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public List<TeamMember> getTeamMembers()
    {
        return teamMembers;
    }

    public void setTeamMembers(List<TeamMember> teamMembers)
    {
        this.teamMembers = teamMembers;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (id == null) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        Player other = ( Player ) obj;
        if ( id == null )
        {
            if ( other.id != null )
                return false;
        }
        else if ( !id.equals( other.id ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + id + ", " + name + "]";
    }
}
