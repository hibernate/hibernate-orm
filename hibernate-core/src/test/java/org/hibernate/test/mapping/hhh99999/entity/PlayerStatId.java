package org.hibernate.test.mapping.hhh99999.entity;

import java.io.Serializable;

public class PlayerStatId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private ScoreId score;

    private TeamMemberId teamMember;

    public PlayerStatId()
    {
    }

    public PlayerStatId(PlayerStatId p)
    {
        this(p.getGameId(), p.getHome(), p.getPlayerId(), p.getRosterId());
    }

    public PlayerStatId(Integer gameId, Boolean home, Integer playerId, Integer rosterId)
    {
        this.score = new ScoreId(gameId, home);
        this.teamMember = new TeamMemberId(playerId, rosterId);
    }

    public Integer getGameId()
    {
        return score.getGameId();
    }

    public void setGameId(Integer gameId)
    {
        score.setGameId(gameId);
    }

    public Boolean getHome()
    {
        return score.getHome();
    }

    public void setHome(Boolean home)
    {
        score.setHome(home);
    }

    public Integer getPlayerId()
    {
        return teamMember.getPlayerId();
    }

    public void setPlayerId(Integer playerId)
    {
        teamMember.setPlayerId(playerId);
    }

    public Integer getRosterId()
    {
        return teamMember.getRosterId();
    }

    public void setRosterId(Integer rosterId)
    {
        teamMember.setRosterId(rosterId);
    }

    public ScoreId getScoreId()
    {
        return score;
    }

    public void setScoreId(ScoreId scoreId)
    {
        this.score = scoreId;
    }

    public TeamMemberId getTeamMemberId()
    {
        return teamMember;
    }

    public void setTeamMemberId(TeamMemberId teamMemberId)
    {
        this.teamMember = teamMemberId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (score == null) ? 0 : score.hashCode() );
        result = prime * result + ( (teamMember == null) ? 0 : teamMember.hashCode() );
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
        PlayerStatId other = ( PlayerStatId ) obj;
        if ( score == null )
        {
            if ( other.score != null )
                return false;
        }
        else if ( !score.equals( other.score ) )
            return false;
        if ( teamMember == null )
        {
            if ( other.teamMember != null )
                return false;
        }
        else if ( !teamMember.equals( other.teamMember ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + score + ", " + teamMember + "]";
    }
}
