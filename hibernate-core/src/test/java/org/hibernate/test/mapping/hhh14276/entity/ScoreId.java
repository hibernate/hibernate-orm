package org.hibernate.test.mapping.hhh14276.entity;

import java.io.Serializable;

public class ScoreId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Integer gameId;

    private Boolean home;

    public ScoreId()
    {
    }

    public Integer getGameId()
    {
        return gameId;
    }

    public void setGameId(Integer gameId)
    {
        this.gameId = gameId;
    }

    public Boolean getHome()
    {
        return home;
    }

    public void setHome(Boolean home)
    {
        this.home = home;
    }
}
