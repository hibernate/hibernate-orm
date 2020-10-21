package org.hibernate.query.hhh13812;

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
@IdClass(SimpleTeamPk.class)
public class SimpleTeam implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "club_id" )
    private Integer clubId;

    @Id
    @Column(name = "team_type_code")
    private String teamTypeCode;

    @Id
    @Column(name = "ordinal_nbr")
    private Integer ordinalNbr;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "club_id", referencedColumnName = "id", insertable = false, updatable = false)
    private SimpleClub club;

    public SimpleTeam()
    {
    }

    public SimpleTeam(Integer clubId, String teamTypeCode, Integer ordinalNbr)
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

    public SimpleClub getClub()
    {
        return club;
    }

    public void setClub(SimpleClub club)
    {
        this.club = club;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( (clubId == null) ? 0 : clubId.hashCode() );
        result = prime * result + ( (teamTypeCode == null) ? 0 : teamTypeCode.hashCode() );
        result = prime * result + ( (ordinalNbr == null) ? 0 : ordinalNbr.hashCode() );
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
        SimpleTeam other = ( SimpleTeam ) obj;
        if ( clubId == null )
        {
            if ( other.clubId != null )
                return false;
        }
        else if ( !clubId.equals( other.clubId ) )
            return false;
        if ( teamTypeCode == null )
        {
            if ( other.teamTypeCode != null )
                return false;
        }
        else if ( !teamTypeCode.equals( other.teamTypeCode ) )
            return false;
        if ( ordinalNbr == null )
        {
            if ( other.ordinalNbr != null )
                return false;
        }
        else if ( !ordinalNbr.equals( other.ordinalNbr ) )
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "Team [clubId=" + clubId + ", teamTypeCode=" + teamTypeCode + ", ordinalNbr=" + ordinalNbr + "]";
    }
}
