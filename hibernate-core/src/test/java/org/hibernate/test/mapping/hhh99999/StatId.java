package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;
import java.util.Objects;

public class StatId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Integer period;

    private PlayerStatId playerStat;

    public StatId()
    {
    }

    public StatId(StatId s)
    {
        this(s.getGameId(), s.getHome(), s.getPlayerId(), s.getRosterId(), s.getPeriod());
    }

    public StatId(Integer gameId, Boolean home, Integer playerId, Integer rosterId, Integer period)
    {
        this.period = Objects.requireNonNull(period);

        this.playerStat = new PlayerStatId(gameId, home, playerId, rosterId);
    }

    public Integer getGameId()
    {
        return playerStat.getGameId();
    }

    public void setGameId(Integer gameId)
    {
        playerStat.setGameId(gameId);
    }

    public Boolean getHome()
    {
        return playerStat.getHome();
    }

    public void setHome(Boolean home)
    {
        playerStat.setHome(home);
    }

    public Integer getPlayerId()
    {
        return playerStat.getPlayerId();
    }

    public void setPlayerId(Integer playerId)
    {
        playerStat.setPlayerId(playerId);
    }

    public Integer getRosterId()
    {
        return playerStat.getRosterId();
    }

    public void setRosterId(Integer rosterId)
    {
        playerStat.setRosterId(rosterId);
    }

    public Integer getPeriod()
    {
        return period;
    }

    public void setPeriod(Integer period)
    {
        this.period = period;
    }

    public PlayerStatId getPlayerStatId()
    {
        return playerStat;
    }

    public void setPlayerStatId(PlayerStatId playerStatId)
    {
        this.playerStat = playerStatId;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (period == null) ? 0 : period.hashCode() );
        result = prime * result + ( (playerStat == null) ? 0 : playerStat.hashCode() );
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
        StatId other = ( StatId ) obj;
        if ( period == null )
        {
            if ( other.period != null )
                return false;
        }
        else if ( !period.equals( other.period ) )
            return false;
        if ( playerStat == null )
        {
            if ( other.playerStat != null )
                return false;
        }
        else if ( !playerStat.equals( other.playerStat ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + period + ", " + playerStat + "]";
    }
}
