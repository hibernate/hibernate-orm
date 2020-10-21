package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "\"Assignments\"")
@IdClass(AssignmentId.class)
public class Assignment implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Basic
    @Column(name = "was_absent")
    private Boolean wasAbsent = Boolean.FALSE;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    private Game game;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "referee_id", referencedColumnName = "referee_id")
    @JoinColumn(name = "club_id", referencedColumnName = "club_id")
    @JoinColumn(name = "season_start_year", referencedColumnName = "season_start_year")
    private RefpoolMember refpoolMember;

    public Assignment()
    {
    }

    public Assignment(Assignment a)
    {
        this(a.getRefereeId(), a.getClubId(), a.getSeasonStartYear(), a.getGameId());

        this.wasAbsent = a.getWasAbsent();
    }

    public Assignment(Integer refereeId, Integer clubId, Integer seasonStartYear, Integer gameId)
    {
        this.game = new Game();
        this.game.setId(gameId);

        this.refpoolMember = new RefpoolMember(refereeId, clubId, seasonStartYear);
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
        return game.getId();
    }

    public void setGameId(Integer gameId)
    {
        game.setId(gameId);
    }

    public Boolean getWasAbsent()
    {
        return wasAbsent;
    }

    public void setWasAbsent(Boolean wasAbsent)
    {
        this.wasAbsent = wasAbsent;
    }

    public Game getGame()
    {
        return game;
    }

    public void setGame(Game game)
    {
        this.game = game;
    }

    public RefpoolMember getRefpoolMember()
    {
        return refpoolMember;
    }

    public void setRefpoolMember(RefpoolMember refpoolMember)
    {
        this.refpoolMember = refpoolMember;
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
        Assignment other = ( Assignment ) obj;
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
        return "[" + wasAbsent + "]";
    }
}
