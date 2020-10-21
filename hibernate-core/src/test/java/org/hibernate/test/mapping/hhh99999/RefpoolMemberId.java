package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;
import java.util.Objects;

public class RefpoolMemberId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Integer club;

    private Integer refereeId;

    private Integer seasonStartYear;

    public RefpoolMemberId()
    {
    }

    public RefpoolMemberId(RefpoolMemberId r)
    {
        this(r.getRefereeId(), r.getClubId(), r.getSeasonStartYear());
    }

    public RefpoolMemberId(Integer refereeId, Integer clubId, Integer seasonStartYear)
    {
        this.club = Objects.requireNonNull(clubId);
        this.refereeId = Objects.requireNonNull(refereeId);
        this.seasonStartYear = Objects.requireNonNull(seasonStartYear);
    }

    public Integer getRefereeId()
    {
        return refereeId;
    }

    public void setRefereeId(Integer refereeId)
    {
        this.refereeId = refereeId;
    }

    public Integer getClubId()
    {
        return club;
    }

    public void setClubId(Integer clubId)
    {
        this.club = clubId;
    }

    public Integer getSeasonStartYear()
    {
        return seasonStartYear;
    }

    public void setSeasonStartYear(Integer seasonStartYear)
    {
        this.seasonStartYear = seasonStartYear;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (club == null) ? 0 : club.hashCode() );
        result = prime * result + ( (refereeId == null) ? 0 : refereeId.hashCode() );
        result = prime * result + ( (seasonStartYear == null) ? 0 : seasonStartYear.hashCode() );
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
        RefpoolMemberId other = ( RefpoolMemberId ) obj;
        if ( club == null )
        {
            if ( other.club != null )
                return false;
        }
        else if ( !club.equals( other.club ) )
            return false;
        if ( refereeId == null )
        {
            if ( other.refereeId != null )
                return false;
        }
        else if ( !refereeId.equals( other.refereeId ) )
            return false;
        if ( seasonStartYear == null )
        {
            if ( other.seasonStartYear != null )
                return false;
        }
        else if ( !seasonStartYear.equals( other.seasonStartYear ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + club + ", " + refereeId + ", " + seasonStartYear + "]";
    }
}
