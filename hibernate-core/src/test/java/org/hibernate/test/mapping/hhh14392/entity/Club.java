package org.hibernate.test.mapping.hhh14392.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "Clubs")
public class Club implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column
    private Integer id;

    @Basic(optional = false)
    @Column
    private String name;

    @Basic(optional = false)
    @Column
    private String code;

    @OneToMany(mappedBy = "club")
    @org.hibernate.annotations.OrderBy(clause = "CASE WHEN SUBSTRING( team_type_code, 1, 1 ) = 'O' THEN -1 ELSE 1 END DESC")  // doesn't work
//    @org.hibernate.annotations.OrderBy(clause = "CASE WHEN SUBSTRING( team_type_code, 1, 1 ) = 'O' THEN -CAST(SUBSTRING( team_type_code, 2, 2 ) AS DECIMAL) ELSE CAST(SUBSTRING( team_type_code, 2, 2 ) AS DECIMAL) END DESC")  // doesn't work
//    @org.hibernate.annotations.OrderBy(clause = "SUBSTRING( team_type_code, 1, 1 ) ASC, CAST(SUBSTRING( team_type_code, 2, 2 ) AS DECIMAL) DESC, SUBSTRING( team_type_code, 4, 1 ) DESC, ordinal_nbr")  // works
//    @org.hibernate.annotations.OrderBy(clause = "SUBSTRING( team_type_code, 1, 1 ) ASC, CASE WHEN SUBSTRING( team_type_code, 1, 1 ) = 'O' THEN -CAST(SUBSTRING( team_type_code, 2, 2 ) AS DECIMAL) ELSE CAST(SUBSTRING( team_type_code, 2, 2 ) AS DECIMAL) END DESC, SUBSTRING( team_type_code, 4, 1 ) DESC, ordinal_nbr")  // doesn't work
    private List<Team> teams = new ArrayList<>();

    public Club()
    {
    }

    public Club(Integer id, String name, String code)
    {
        this.id = Objects.requireNonNull(id);
        this.name = name;
        this.code = code;
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

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public List<Team> getTeams()
    {
        return teams;
    }

    public void setTeams(List<Team> teams)
    {
        this.teams = teams;
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        Club other = ( Club ) obj;

        return Objects.equals(this.id, other.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.id);
    }

    @Override
    public String toString()
    {
        return "Club [id=" + this.id + ", name=" + this.name + ", code=" + this.code + "]";
    }
}
