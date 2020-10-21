package org.hibernate.test.mapping.hhh99999;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "\"TeamMembers\"")
@IdClass(TeamMemberId.class)
public class TeamMember implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "roster_id")
    private Roster roster;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private Player player;

    @OneToMany(mappedBy = "teamMember")
    private List<PlayerStat> playerStats;

    public TeamMember()
    {
    }

    public TeamMember(TeamMember t)
    {
        this(t.getPlayerId(), t.getRosterId());
    }

    public TeamMember(Integer playerId, Integer rosterId)
    {
        this.roster = new Roster();
        this.roster.setId(rosterId);

        this.player = new Player(playerId);
    }

    public Integer getPlayerId()
    {
        return player.getId();
    }

    public void setPlayerId(Integer playerId)
    {
        player.setId(playerId);
    }

    public Integer getRosterId()
    {
        return roster.getId();
    }

    public void setRosterId(Integer rosterId)
    {
        roster.setId(rosterId);
    }

    public Roster getRoster()
    {
        return roster;
    }

    public void setRoster(Roster roster)
    {
        this.roster = roster;
    }

    public Player getPlayer()
    {
        return player;
    }

    public void setPlayer(Player player)
    {
        this.player = player;
    }

    public List<PlayerStat> getPlayerStats()
    {
        return playerStats;
    }

    public void setPlayerStats(List<PlayerStat> playerStats)
    {
        this.playerStats = playerStats;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (player == null) ? 0 : player.hashCode() );
        result = prime * result + ( (roster == null) ? 0 : roster.hashCode() );
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
        TeamMember other = ( TeamMember ) obj;
        if ( player == null )
        {
            if ( other.player != null )
                return false;
        }
        else if ( !player.equals( other.player ) )
            return false;
        if ( roster == null )
        {
            if ( other.roster != null )
                return false;
        }
        else if ( !roster.equals( other.roster ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "[" + "]";
    }
}
