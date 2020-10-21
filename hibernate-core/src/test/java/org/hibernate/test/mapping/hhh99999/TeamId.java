package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;
import java.util.Objects;

public class TeamId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Integer club;

    private String teamTypeCode;

    private Integer ordinalNbr;

    public TeamId()
    {
    }

    public TeamId(TeamId t)
    {
        this(t.getClubId(), t.getTeamTypeCode(), t.getOrdinalNbr());
    }

    public TeamId(Integer clubId, String teamTypeCode, Integer ordinalNbr)
    {
        this.club = Objects.requireNonNull(clubId);
        this.teamTypeCode = Objects.requireNonNull(teamTypeCode);
        this.ordinalNbr = Objects.requireNonNull(ordinalNbr);
    }

    public Integer getClubId()
    {
        return club;
    }

    public void setClubId(Integer clubId)
    {
        this.club = clubId;
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
        TeamId other = ( TeamId ) obj;
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
        return "[" + club + ", " + teamTypeCode + ", " + ordinalNbr + "]";
    }
}
