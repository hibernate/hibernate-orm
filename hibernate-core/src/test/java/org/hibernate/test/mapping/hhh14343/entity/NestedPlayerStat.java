package org.hibernate.test.mapping.hhh14343.entity;

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
@Table(name = "\"PlayerStats\"")
@IdClass(NestedPlayerStatId.class)
public class NestedPlayerStat implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "player_id")
    private Integer playerId;

    @Basic(optional = false)
    @Column(name = "jersey_nbr")
    private Integer jerseyNbr;

    @Id
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "game_id", referencedColumnName = "game_id")
    @JoinColumn(name = "is_home", referencedColumnName = "is_home")
    private NestedScore score;

    public NestedPlayerStat()
    {
    }

    public Integer getPlayerId()
    {
        return playerId;
    }

    public void setPlayerId(Integer playerId)
    {
        this.playerId = playerId;
    }

    public Integer getJerseyNbr()
    {
        return jerseyNbr;
    }

    public void setJerseyNbr(Integer jerseyNbr)
    {
        this.jerseyNbr = jerseyNbr;
    }

    public NestedScore getScore()
    {
        return score;
    }

    public void setScore(NestedScore score)
    {
        this.score = score;
    }
}
