package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;
import java.util.Objects;

public class AssignmentId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Integer game;

    private RefpoolMemberId refpoolMember;

    public AssignmentId()
    {
    }

    public AssignmentId(AssignmentId a)
    {
        this(a.getRefereeId(), a.getClubId(), a.getSeasonStartYear(), a.getGameId());
    }

    public AssignmentId(Integer refereeId, Integer clubId, Integer seasonStartYear, Integer gameId)
    {
        this.game = Objects.requireNonNull(gameId);

        this.refpoolMember = new RefpoolMemberId(refereeId, clubId, seasonStartYear);
    }

    public Integer getRefereeId()
    {
        return refpoolMember.getRefereeId();
    }

    public void setRefereeId(Integer refereeId)
    {
        refpoolMember.setRefereeId(refereeId);
    }

    public Integer getClubId()
    {
        return refpoolMember.getClubId();
    }

    public void setClubId(Integer clubId)
    {
        refpoolMember.setClubId(clubId);
    }

    public Integer getSeasonStartYear()
    {
        return refpoolMember.getSeasonStartYear();
    }

    public void setSeasonStartYear(Integer seasonStartYear)
    {
        refpoolMember.setSeasonStartYear(seasonStartYear);
    }

    public Integer getGameId()
    {
        return game;
    }

    public void setGameId(Integer gameId)
    {
        this.game = gameId;
    }

    public RefpoolMemberId getRefpoolMemberId()
    {
        return refpoolMember;
    }

    public void setRefpoolMemberId(RefpoolMemberId refpoolMemberId)
    {
        this.refpoolMember = refpoolMemberId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (game == null) ? 0 : game.hashCode() );
        result = prime * result + ( (refpoolMember == null) ? 0 : refpoolMember.hashCode() );
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
        AssignmentId other = ( AssignmentId ) obj;
        if ( game == null )
        {
            if ( other.game != null )
                return false;
        }
        else if ( !game.equals( other.game ) )
            return false;
        if ( refpoolMember == null )
        {
            if ( other.refpoolMember != null )
                return false;
        }
        else if ( !refpoolMember.equals( other.refpoolMember ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + game + ", " + refpoolMember + "]";
    }
}
