package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "\"Teams\"")
@IdClass(TeamId.class)
public class Team implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "team_type_code")
    private String teamTypeCode;

    @Id
    @Column(name = "ordinal_nbr")
    private Integer ordinalNbr;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id")
    private Club club;

    @OneToMany(mappedBy = "team")
    private List<Roster> rosters;

    public Team()
    {
    }

    public Team(Team t)
    {
        this(t.getClubId(), t.getTeamTypeCode(), t.getOrdinalNbr());
    }

    public Team(Integer clubId, String teamTypeCode, Integer ordinalNbr)
    {
        this.teamTypeCode = Objects.requireNonNull(teamTypeCode);
        this.ordinalNbr = Objects.requireNonNull(ordinalNbr);

        this.club = new Club(clubId);
    }

    public Integer getClubId()
    {
        return club.getId();
    }

    public void setClubId(Integer clubId)
    {
        club.setId(clubId);
    }

    public String getTeamTypeCode()
    {
        return teamTypeCode;
    }

    public void setTeamTypeCode(String teamTypeCode)
    {
        this.teamTypeCode = teamTypeCode;
    }

    public Integer getOrdinalNbr()
    {
        return ordinalNbr;
    }

    public void setOrdinalNbr(Integer ordinalNbr)
    {
        this.ordinalNbr = ordinalNbr;
    }

    public Club getClub()
    {
        return club;
    }

    public void setClub(Club club)
    {
        this.club = club;
    }

    public List<Roster> getRosters()
    {
        return rosters;
    }

    public void setRosters(List<Roster> rosters)
    {
        this.rosters = rosters;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (club == null) ? 0 : club.hashCode() );
        result = prime * result + ( (ordinalNbr == null) ? 0 : ordinalNbr.hashCode() );
        result = prime * result + ( (teamTypeCode == null) ? 0 : teamTypeCode.hashCode() );
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
        Team other = ( Team ) obj;
        if ( club == null )
        {
            if ( other.club != null )
                return false;
        }
        else if ( !club.equals( other.club ) )
            return false;
        if ( ordinalNbr == null )
        {
            if ( other.ordinalNbr != null )
                return false;
        }
        else if ( !ordinalNbr.equals( other.ordinalNbr ) )
            return false;
        if ( teamTypeCode == null )
        {
            if ( other.teamTypeCode != null )
                return false;
        }
        else if ( !teamTypeCode.equals( other.teamTypeCode ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + teamTypeCode + ", " + ordinalNbr + "]";
    }
}
