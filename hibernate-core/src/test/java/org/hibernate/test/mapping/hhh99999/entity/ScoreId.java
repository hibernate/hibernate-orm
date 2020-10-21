package org.hibernate.test.mapping.hhh99999.entity;

import java.io.Serializable;
import java.util.Objects;

public class ScoreId implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Integer game;

    private Boolean home;

    public ScoreId()
    {
    }

    public ScoreId(ScoreId s)
    {
        this(s.getGameId(), s.getHome());
    }

    public ScoreId(Integer gameId, Boolean home)
    {
        this.game = Objects.requireNonNull(gameId);
        this.home = Objects.requireNonNull(home);
    }

    public Integer getGameId()
    {
        return game;
    }

    public void setGameId(Integer gameId)
    {
        this.game = gameId;
    }

    public Boolean getHome()
    {
        return home;
    }

    public void setHome(Boolean home)
    {
        this.home = home;
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
        ScoreId other = ( ScoreId ) obj;
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
        return "[" + game + ", " + home + "]";
    }
}
