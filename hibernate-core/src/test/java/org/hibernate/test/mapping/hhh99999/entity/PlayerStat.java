package org.hibernate.test.mapping.hhh99999.entity;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "\"PlayerStats\"")
@IdClass(PlayerStatId.class)
public class PlayerStat implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "player_id")
    private Integer playerId;

    @Id
    @Column(name = "roster_id")
    private Integer rosterId;

    @Basic(optional = false)
    @Column(name = "jersey_nbr")
    private Integer jerseyNbr;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id", referencedColumnName = "game_id")
    @JoinColumn(name = "is_home", referencedColumnName = "is_home")
    private Score score;

    @OneToMany(mappedBy = "playerStat")
    @OrderBy("period")
    private List<Stat> stats;

    public PlayerStat()
    {
    }

    public PlayerStat(PlayerStat p)
    {
        this(p.getGameId(), p.getHome(), p.getPlayerId(), p.getRosterId(), p.getJerseyNbr());
    }

    public PlayerStat(Integer jerseyNbr)
    {
        this(null, null, null, null, jerseyNbr);
    }

    public PlayerStat(Integer gameId, Boolean home, Integer playerId, Integer rosterId)
    {
        this(gameId, home, playerId, rosterId, null);
    }

    public PlayerStat(Integer gameId, Boolean home, Integer playerId, Integer rosterId, Integer jerseyNbr)
    {
        this.playerId = Objects.requireNonNull(playerId);
        this.rosterId = Objects.requireNonNull(rosterId);
        this.jerseyNbr = jerseyNbr;

        this.score = new Score(gameId, home);
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
        return playerId;
    }

    public void setPlayerId(Integer playerId)
    {
        this.playerId = playerId;
    }

    public Integer getRosterId()
    {
        return rosterId;
    }

    public void setRosterId(Integer rosterId)
    {
        this.rosterId = rosterId;
    }

    public Integer getJerseyNbr()
    {
        return jerseyNbr;
    }

    public void setJerseyNbr(Integer jerseyNbr)
    {
        this.jerseyNbr = jerseyNbr;
    }

    public Score getScore()
    {
        return score;
    }

    public void setScore(Score score)
    {
        this.score = score;
    }

    public List<Stat> getStats()
    {
        return stats;
    }

    public void setStats(List<Stat> stats)
    {
        this.stats = stats;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (playerId == null) ? 0 : playerId.hashCode() );
        result = prime * result + ( (rosterId == null) ? 0 : rosterId.hashCode() );
        result = prime * result + ( (score == null) ? 0 : score.hashCode() );
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
        PlayerStat other = ( PlayerStat ) obj;
        if ( playerId == null )
        {
            if ( other.playerId != null )
                return false;
        }
        else if ( !playerId.equals( other.playerId ) )
            return false;
        if ( rosterId == null )
        {
            if ( other.rosterId != null )
                return false;
        }
        else if ( !rosterId.equals( other.rosterId ) )
            return false;
        if ( score == null )
        {
            if ( other.score != null )
                return false;
        }
        else if ( !score.equals( other.score ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + playerId + ", " + rosterId + ", " + jerseyNbr + "]";
    }
}
