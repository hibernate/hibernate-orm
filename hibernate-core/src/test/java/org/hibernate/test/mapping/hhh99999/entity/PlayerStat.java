package org.hibernate.test.mapping.hhh99999.entity;

import java.io.Serializable;
import java.util.List;

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

    @Basic(optional = false)
    @Column(name = "jersey_nbr")
    private Integer jerseyNbr;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id", referencedColumnName = "game_id")
    @JoinColumn(name = "is_home", referencedColumnName = "is_home")
    private Score score;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", referencedColumnName = "player_id")
    @JoinColumn(name = "roster_id", referencedColumnName = "roster_id")
    private TeamMember teamMember;

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
        this.jerseyNbr = jerseyNbr;

        this.score = new Score(gameId, home);
        this.teamMember = new TeamMember(playerId, rosterId);
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

    public TeamMember getTeamMember()
    {
        return teamMember;
    }

    public void setTeamMember(TeamMember teamMember)
    {
        this.teamMember = teamMember;
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
        PlayerStat other = ( PlayerStat ) obj;
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
        return "[" + jerseyNbr + "]";
    }
}
