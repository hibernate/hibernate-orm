package org.hibernate.test.mapping.hhh99999.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "\"Rosters\"")
public class Roster implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id", referencedColumnName = "club_id")
    @JoinColumn(name = "team_type_code", referencedColumnName = "team_type_code")
    @JoinColumn(name = "team_ordinal_nbr", referencedColumnName = "ordinal_nbr")
    private Team team;

    @OneToMany(mappedBy = "roster")
    private List<Score> scores;

    @OneToMany(mappedBy = "roster")
    @OrderBy
    private List<TeamMember> teamMembers;

    public Roster()
    {
    }

    public Roster(Roster r)
    {
        this(r.getClubId(), r.getTeamTypeCode(), r.getTeamOrdinalNbr());

        this.id = Objects.requireNonNull(r.getId());
    }

    public Roster(Integer clubId, String teamTypeCode, Integer teamOrdinalNbr)
    {
        this.team = new Team(clubId, teamTypeCode, teamOrdinalNbr);
    }

    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Integer getClubId()
    {
        return team.getClubId();
    }

    public void setClubId(Integer clubId)
    {
        team.setClubId(clubId);
    }

    public String getTeamTypeCode()
    {
        return team.getTeamTypeCode();
    }

    public void setTeamTypeCode(String teamTypeCode)
    {
        team.setTeamTypeCode(teamTypeCode);
    }

    public Integer getTeamOrdinalNbr()
    {
        return team.getOrdinalNbr();
    }

    public void setTeamOrdinalNbr(Integer teamOrdinalNbr)
    {
        team.setOrdinalNbr(teamOrdinalNbr);
    }

    public Team getTeam()
    {
        return team;
    }

    public void setTeam(Team team)
    {
        this.team = team;
    }

    public List<Score> getScores()
    {
        return scores;
    }

    public void setScores(List<Score> scores)
    {
        this.scores = scores;
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
        Roster other = ( Roster ) obj;
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
        return "[" + id + "]";
    }
}
