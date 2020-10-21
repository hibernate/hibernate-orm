package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;
import java.util.Objects;

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
@Table(name = "\"Stats\"")
@IdClass(StatId.class)
public class Stat implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column
    private Integer period;

    @Basic(optional = false)
    @Column
    private Integer pts;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id", referencedColumnName = "game_id")
    @JoinColumn(name = "is_home", referencedColumnName = "is_home")
    @JoinColumn(name = "player_id", referencedColumnName = "player_id")
    @JoinColumn(name = "roster_id", referencedColumnName = "roster_id")
    private PlayerStat playerStat;

    public Stat()
    {
    }

    public Stat(Stat s)
    {
        this(s.getGameId(), s.getHome(), s.getPlayerId(), s.getRosterId(), s.getPeriod(), s.getPts());
    }

    public Stat(Integer pts)
    {
        this(null, null, null, null, null, pts);
    }

    public Stat(Integer gameId, Boolean home, Integer playerId, Integer rosterId, Integer period)
    {
        this(gameId, home, playerId, rosterId, period, null);
    }

    public Stat(Integer gameId, Boolean home, Integer playerId, Integer rosterId, Integer period, Integer pts)
    {
        this.period = Objects.requireNonNull(period);
        this.pts = pts;

        this.playerStat = new PlayerStat(gameId, home, playerId, rosterId);
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

    public Integer getPts()
    {
        return pts;
    }

    public void setPts(Integer pts)
    {
        this.pts = pts;
    }

    public PlayerStat getPlayerStat()
    {
        return playerStat;
    }

    public void setPlayerStat(PlayerStat playerStat)
    {
        this.playerStat = playerStat;
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
        Stat other = ( Stat ) obj;
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
        return "[" + period + ", " + pts + "]";
    }
}
