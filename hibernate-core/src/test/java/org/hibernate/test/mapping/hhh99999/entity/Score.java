package org.hibernate.test.mapping.hhh99999.entity;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

@Entity
@Table(name = "\"Scores\"")
@IdClass(ScoreId.class)
public class Score implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "is_home", insertable = false, updatable = false)
    private Boolean home;

    @Basic(optional = false)
    @Column(name = "roster_id")
    private Integer rosterId;

    @Basic
    @Column(name = "final_score")
    private Integer finalScore;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id")
    private Game game;

    @OneToMany(mappedBy = "score")
    @MapKey(name = "jerseyNbr")
    @OrderBy("starter DESC, jerseyNbr")
    private Map<Integer, PlayerStat> playerStats;

    public Score()
    {
    }

    public Score(Score s)
    {
        this(s.getGameId(), s.getHome(), s.getRosterId(), s.getFinalScore());
    }

    public Score(Integer gameId, Boolean home)
    {
        this(gameId, home, null);
    }

    public Score(Integer rosterId, Integer finalScore)
    {
        this(null, null, rosterId, finalScore);
    }

    public Score(Integer gameId, Boolean home, Integer rosterId)
    {
        this(gameId, home, rosterId, null);
    }

    public Score(Integer gameId, Boolean home, Integer rosterId, Integer finalScore)
    {
        this.home = Objects.requireNonNull(home);
        this.rosterId = rosterId;
        this.finalScore = finalScore;

        this.game = new Game();
        this.game.setId(gameId);
    }

    public Integer getGameId()
    {
        return game.getId();
    }

    public void setGameId(Integer gameId)
    {
        game.setId(gameId);
    }

    public Boolean getHome()
    {
        return home;
    }

    public void setHome(Boolean home)
    {
        this.home = home;
    }

    public Integer getRosterId()
    {
        return rosterId;
    }

    public void setRosterId(Integer rosterId)
    {
        this.rosterId = rosterId;
    }

    public Integer getFinalScore()
    {
        return finalScore;
    }

    public void setFinalScore(Integer finalScore)
    {
        this.finalScore = finalScore;
    }

    public Game getGame()
    {
        return game;
    }

    public void setGame(Game game)
    {
        this.game = game;
    }

    public Map<Integer, PlayerStat> getPlayerStats()
    {
        return playerStats;
    }

    public void setPlayerStats(Map<Integer, PlayerStat> playerStats)
    {
        this.playerStats = playerStats;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (game == null) ? 0 : game.hashCode() );
        result = prime * result + ( (home == null) ? 0 : home.hashCode() );
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
        Score other = ( Score ) obj;
        if ( game == null )
        {
            if ( other.game != null )
                return false;
        }
        else if ( !game.equals( other.game ) )
            return false;
        if ( home == null )
        {
            if ( other.home != null )
                return false;
        }
        else if ( !home.equals( other.home ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + home + ", " + rosterId + ", " + finalScore + "]";
    }
}
