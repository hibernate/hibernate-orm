package org.hibernate.test.mapping.hhh14276.entity;

import java.io.Serializable;

public class PlayerStatId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Integer playerId;

    private ScoreId score;

    public PlayerStatId()
    {
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

    public ScoreId getScoreId()
    {
        return score;
    }

    public void setScoreId(ScoreId scoreId)
    {
        this.score = scoreId;
    }
}
