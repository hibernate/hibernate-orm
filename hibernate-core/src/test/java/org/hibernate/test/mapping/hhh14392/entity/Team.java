package org.hibernate.test.mapping.hhh14392.entity;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "Teams")
@IdClass(TeamPk.class)
public class Team implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "club_id")
    private Integer clubId;

    @Id
    @Column(name = "team_type_code")
    private String teamTypeCode;

    @Id
    @Column(name = "ordinal_nbr")
    private Integer ordinalNbr;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id", insertable = false, updatable = false)
    private Club club;

    public Team()
    {
    }

    public Team(Integer clubId, String teamTypeCode, Integer ordinalNbr)
    {
        this.clubId = Objects.requireNonNull(clubId);
        this.teamTypeCode = Objects.requireNonNull(teamTypeCode);
        this.ordinalNbr = Objects.requireNonNull(ordinalNbr);

        this.club = new Club();
        this.club.setId(clubId);
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

    public Club getClub()
    {
        return club;
    }

    public void setClub(Club club)
    {
        this.club = club;
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

        Team other = ( Team ) obj;

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
        return "Team [clubId=" + this.clubId + ", teamTypeCode=" + this.teamTypeCode + ", ordinalNbr=" + this.ordinalNbr + "]";
    }
}
