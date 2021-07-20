package org.hibernate.test.mapping.hhh14392.entity;

import java.io.Serializable;
import java.util.Objects;

public class TeamPk implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Integer clubId;

    private String teamTypeCode;

    private Integer ordinalNbr;

    public TeamPk()
    {
    }

    public TeamPk(Integer clubId, String teamTypeCode, Integer ordinalNbr)
    {
        this.clubId = Objects.requireNonNull(clubId);
        this.teamTypeCode = Objects.requireNonNull(teamTypeCode);
        this.ordinalNbr = Objects.requireNonNull(ordinalNbr);
    }

    public Integer getClubId()
    {
        return clubId;
    }

    public void setClubId(Integer clubId)
    {
        this.clubId = clubId;
    }

    public String getTeamTypeCode()
    {
        return teamTypeCode;
    }

    public void setTeamTypeCode(String teamTypeCode)
    {
        this.teamTypeCode = teamTypeCode;
    }

    public Integer getOrdinalNbr()
    {
        return ordinalNbr;
    }

    public void setOrdinalNbr(Integer ordinalNbr)
    {
        this.ordinalNbr = ordinalNbr;
    }

    @Override
    public boolean equals(Object obj)
    {
        if ( obj == null )
        {
            return false;
        }

        if ( getClass() != obj.getClass() )
        {
            return false;
        }

        TeamPk other = ( TeamPk ) obj;

        return Objects.equals(this.clubId, other.clubId) && Objects.equals(this.teamTypeCode, other.teamTypeCode) && Objects.equals(this.ordinalNbr, other.ordinalNbr);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.clubId, this.teamTypeCode, this.ordinalNbr);
    }

    @Override
    public String toString()
    {
        return "TeamPk [clubId=" + this.clubId + ", teamTypeCode=" + this.teamTypeCode + ", ordinalNbr=" + this.ordinalNbr + "]";
    }
}
