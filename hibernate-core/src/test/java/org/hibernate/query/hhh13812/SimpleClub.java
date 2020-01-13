package org.hibernate.query.hhh13812;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "Clubs")
public class SimpleClub implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column
    protected Integer id;

    @Basic(optional = false)
    @Column
    private String name;

    @Basic(optional = false)
    @Column
    private String code;

//    @OrderBy("SUBSTRING( teamTypeCode, 1, 1) ASC, SUBSTRING( teamTypeCode, 2, 2 ) DESC, SUBSTRING( teamTypeCode, 4, 1 ) DESC, ordinalNbr")  // works, but not what we want
    @OrderBy("SUBSTRING( teamTypeCode, 1, 1) ASC, CASE WHEN SUBSTRING( teamTypeCode, 1, 1 ) = 'O' THEN -CAST(SUBSTRING( teamTypeCode, 2, 2 ) AS DECIMAL) ELSE CAST(SUBSTRING( teamTypeCode, 2, 2 ) AS DECIMAL) END DESC, SUBSTRING( teamTypeCode, 4, 1 ) DESC, ordinalNbr")  // breaks
    @OneToMany(mappedBy = "club")
    private List<SimpleTeam> teams = new ArrayList<>();

    public SimpleClub()
    {
    }

    public SimpleClub(String name, String code)
    {
        this.name = Objects.requireNonNull(name);
        this.code = Objects.requireNonNull(code);
    }

    public Integer getId()
    {
        return id;
    }

    public void setId( Integer id )
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

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public List<SimpleTeam> getTeams()
    {
        return teams;
    }

    public void setTeams(List<SimpleTeam> teams)
    {
        this.teams = teams;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        SimpleClub other = ( SimpleClub ) obj;
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
        return "SimpleClub [id=" + id + ", name=" + name + ", code=" + code + "]";
    }
}
