// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.ejb.test.metadata;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Formula;

@Entity
@Table(name = "PRCONTRA", schema = "MUVALE")
public class Prcontra implements Serializable {
	private static final long serialVersionUID = -4996315772144793755L;
	@EmbeddedId
	protected PrcontraPK prcontraPK;
	@Column(name = "DOC", nullable = false, insertable = false, updatable = false)
	private BigInteger doc;
	@Column(name = "NIF", nullable = false, insertable = false, updatable = false)
	private String nif;
	@Column(name = "ANNO")
	private BigInteger anno;
//	@Column(name = "NIF",insertable=false, updatable=false,nullable=false)
	//	private String nif;
	@Column(name = "PRSSEM")
	private BigInteger prssem;
	@Column(name = "NISSEM")
	private BigInteger nissem;
	@Column(name = "COSSEM")
	private BigInteger cossem;
	@Column(name = "REGIMEN")
	private BigInteger regimen;
	@Column(name = "CONTACTO")
	private String contacto;
	@Column(name = "TELEFONO")
	private String telefono;
	@Column(name = "FAX")
	private String fax;
	@Column(name = "NUTRA")
	private BigInteger nutra;
	@Column(name = "NUTRAT")
	private BigInteger nutrat;
	@Column(name = "CNAE")
	private BigInteger cnae;
	@Column(name = "GREMP")
	private BigInteger gremp;
	@Column(name = "A1")
	private Character a1;
	@Column(name = "F_ALTA")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fAlta;
	@Column(name = "F_BAJA")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fBaja;
	@Column(name = "PRECIO")
	private Float precio;
	@Column(name = "MPAGO")
	private BigInteger mpago;
	@Column(name = "PERIO")
	private BigInteger perio;
	@Column(name = "CTABANCO")
	private String ctabanco;
	@Column(name = "QUIENFIRMA")
	private String quienfirma;
	@Column(name = "DNIFIRMA")
	private String dnifirma;
	@Column(name = "CARGOFIRMA")
	private String cargofirma;
	@Column(name = "F_ESCRITURA")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fEscritura;
	@Column(name = "NOTARIO")
	private String notario;
	@Column(name = "POBLANOT")
	private String poblanot;
	@Column(name = "PROTOCOLO")
	private String protocolo;
	@Column(name = "LUGARFIRMA")
	private String lugarfirma;
	@Column(name = "CONTF")
	private BigInteger contf;
	@Column(name = "BLOQUEO")
	private Character bloqueo;
	@Column(name = "FACTURA")
	private Character factura;
	@Column(name = "ENPODER")
	private String enpoder;
	@Column(name = "CPAR1")
	private Character cpar1;
	@Column(name = "CPAR2")
	private Character cpar2;
	@Column(name = "CPAR3")
	private Character cpar3;
	@Column(name = "CPAR4")
	private Character cpar4;
	@Column(name = "CPAR5")
	private Character cpar5;
	@Column(name = "CPAR6")
	private Character cpar6;
	@Column(name = "CPAR7")
	private Character cpar7;
	@Column(name = "CPAR8")
	private Character cpar8;
	@Column(name = "CPAR9")
	private Character cpar9;
	@Column(name = "SPP1")
	private Character spp1;
	@Column(name = "SPP2")
	private Character spp2;
	@Column(name = "SPP3")
	private Character spp3;
	@Column(name = "RR")
	private Character rr;
	@Column(name = "TIPOC")
	private String tipoc;
	@Column(name = "PTRAB")
	private String ptrab;
	@Column(name = "PCURSO")
	private String pcurso;
	@Column(name = "USUALT")
	private BigInteger usualt;
	@Column(name = "F_USU")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fUsu;
	@Column(name = "FIRMADO")
	private Character firmado;
	@Column(name = "VS")
	private Character vs;
	@Column(name = "CENTRA")
	private BigInteger centra;
	@Column(name = "RT")
	private BigInteger rt;
	@Column(name = "F1")
	private Character f1;
	@Column(name = "IPC")
	private Character ipc;
	@Column(name = "NR")
	private BigInteger nr;
	@Column(name = "COORD")
	private BigInteger coord;
	@Column(name = "OBSERV")
	private String observ;
	@Column(name = "TM")
	private BigInteger tm;
	@Column(name = "RE")
	private BigInteger re;
	@Column(name = "UMOVIL")
	private Character umovil;
	@Column(name = "ADS")
	private BigInteger ads;
	@Column(name = "F_DESCARGA")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fDescarga;
	@Column(name = "GRUPO_EMP")
	private BigInteger grupoEmp;
	@Column(name = "OBSERV1")
	private String observ1;
	@Column(name = "OBSERV2")
	private String observ2;
	@Column(name = "F_LLAMADA1")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fLlamada1;
	@Column(name = "NA1")
	private Character na1;
	@Column(name = "CONST")
	private Character const1;
	@Column(name = "F_EVR")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fEvr;
	@Column(name = "F_PLAN")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fPlan;
	@Column(name = "F_MEMORIA")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fMemoria;
	@Column(name = "F_RENOVE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fRenove;
	@Column(name = "INCI")
	private Character inci;
	@Column(name = "F_ENV_PLAN")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fEnvPlan;
	@Column(name = "A1_DOC_IMP")
	private Float a1DocImp;
	@Column(name = "A1_DOC_NTRA")
	private Float a1DocNtra;
	@Column(name = "A1_RM_IMP")
	private Float a1RmImp;
	@Column(name = "A1_RM_NTRA")
	private Float a1RmNtra;
	@Column(name = "NA1_DOC_IMP")
	private Float na1DocImp;
	@Column(name = "NA1_DOC_NTRA")
	private Float na1DocNtra;
	@Column(name = "NA1_RM_IMP")
	private Float na1RmImp;
	@Column(name = "NA1_RM_NTRA")
	private Float na1RmNtra;
	@Column(name = "CONST_DOC_IMP")
	private Float constDocImp;
	@Column(name = "CONST_DOC_NTRA")
	private Float constDocNtra;
	@Column(name = "CONST_RM_IMP")
	private Float constRmImp;
	@Column(name = "CONST_RM_NTRA")
	private String constRmNtra;
	@Column(name = "TIENE_PRE")
	private Character tienePre;
	@Column(name = "ACEPTA_PRE")
	private Character aceptaPre;
	@Column(name = "F_PRE")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fPre;
	@Column(name = "NHORAS")
	private BigInteger nhoras;
	@Column(name = "CHORA")
	private Float chora;
	@Column(name = "IND_UNICO")
	private BigInteger indUnico;
	@Column(name = "NUMEROI")
	private BigInteger numeroi;
	@Column(name = "RENOVI")
	private BigInteger renovi;
	@Column(name = "TIPOI")
	private BigInteger tipoi;
	@Column(name = "TARIFA")
	private Float tarifa;
	@Column(name = "IPC_PORC")
	private Float ipcPorc;
	@Column(name = "F_REC_MEM")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fRecMem;
	@Column(name = "F_REC_PLAN")
	@Temporal(TemporalType.TIMESTAMP)
	private Date fRecPlan;
	@Column(name = "IPC_IMP")
	private Float ipcImp;
	@Column(name = "UMOVIL_IMP")
	private Float umovilImp;
	@Column(name = "QUIENFIRMA2")
	private String quienfirma2;
	@Column(name = "DNIFIRMA2")
	private String dnifirma2;
	@Column(name = "CARGOFIRMA2")
	private String cargofirma2;
	@Column(name = "DOMIC_ENV")
	private String domicEnv;
	@Column(name = "PROVI_ENV")
	private BigInteger proviEnv;
	@Column(name = "POBLA_ENV")
	private BigInteger poblaEnv;
	@Column(name = "DISPO_ENV")
	private BigInteger dispoEnv;
	@Column(name = "MUT_ORIGEN")
	private BigInteger mutOrigen;
	@Column(name = "COD_CONCIERTO")
	private String codConcierto;
	@Column(name = "N_PEDIDO")
	private String nPedido;
	@Column(name = "D_FACTURA")
	private BigInteger dFactura;
	@Column(name = "D_VENCIMIENTO")
	private String dVencimiento;
	@Column(name = "CON_NRM_RH")
	private BigInteger conNrmRh;
	@Column(name = "CON_NRM_RT")
	private BigInteger conNrmRt;

	@Column(name = "CON_PRECIO_RM_EXCESO")
	private Float conPrecioRmExceso;
	@Column(name = "CON_NTRA_DOC_RH")
	private BigInteger conNtraDocRh;
	@Column(name = "CON_IMPDOC_RHIGIENICO")
	private Float conImpDocRhigienico;
	@Column(name = "CON_IMPRM_RHIGIENICO")
	private Float conImprmRhigienico;
	@Column(name = "CON_NTRA_DOC_RT")
	private BigInteger conNtraDocRt;
	@Column(name = "CON_IMPDOC_RTIPOLOGIAS")
	private Float conImpdocRTipologias;
	@Column(name = "CON_IMPRM_RTIPOLOGIAS")
	private Float conImprmRtipologias;
	@Column(name = "CON_IMP_UMOVIL")
	private Float conImpUmovil;
	@Column(name = "CON_PRECIO_SME")
	private Float conPrecioSme;


	@Formula(value = "(select pago.texto from muvale.mpago pago  where pago.mpago=mpago)")
	private String textoMpago;

	@Formula(value = "(" +
			"muvale.nutra_nif(" +
			"		to_char(F_ALTA,'RRRR')," +
			"		to_char(F_ALTA,'MM')," +
			"		nif) " +
			")")
	private Integer totalAfiliados;

	// Modo de pago
	@Formula(value = "(select p.DOM_DESCRIPCION  from prevencion.prv_dominios_prevencion p where p.dom_campo='TPRECIO' and p.dom_valor=tarifa)")
	private String modo;

	// Impagado
	@Formula(value = "( SELECT distinct 1 " +
			"   FROM MUVALE.PRFAC A " +
			"  inner join MUVALE.PRECIB R on  A.ANNO = R.ANNO and A.NUMFAC = R.NUMFAC and  A.SERIE  = R.SERIE" +
			"  WHERE A.SERIE = 10" +
			"    AND R.NUM = (SELECT MAX(R1.NUM) " +
			"                   FROM MUVALE.PRECIB R1 " +
			"                  WHERE R.ANNO = R1.ANNO " +
			"                    AND R.NUMFAC = R1.NUMFAC " +
			"                    AND R.SERIE = R1.SERIE " +
			"                    AND R.LIN = R1.LIN) " +
			"    AND R.COBRADO IN ('I','J') " +
			"    and a.numero = numero  and a.renov = renov  and a.tipo = tipo)")
	private Integer impagado;

	@Formula(value = "( " +
			" select count( r.rec_codigo )  " +
			"   from prevencion.vds_reconocimiento r " +
			"  where r.rec_numero= NUMERO " +
			"    and r.rec_tipo = 2 " +
			"    and r.rec_fecha between F_ALTA and nvl(F_BAJA,sysdate) " +
			"    and r.rec_estado>0 " +
			"    and r.rec_tiprec in(0,1,3,10,15,16,17) )")
	//Los mismos que en VdsReconocimientoDAO.FILTRO_TIPO_DE_RECONOCIMENTO
	private Long numReconRealizados;

	@Formula(value = "( " +
			" nvl(CON_NRM_RH,0) + nvl(CON_NRM_RT,0) " +
			" )")
	private Long numReconPrevistos;

	//END-ENTITY-MOD


	public Prcontra() {
	}

	public Prcontra(PrcontraPK prcontraPK) {
		this.prcontraPK = prcontraPK;

	}

	public Prcontra(BigInteger numero, BigInteger renov, BigInteger tipo) {
		this.prcontraPK = new PrcontraPK( numero, renov, tipo );
	}

	public Prcontra(BigInteger numero, BigInteger renov, BigInteger tipo, Date fAlta, Character bloqueo) {
		this.prcontraPK = new PrcontraPK( numero, renov, tipo );
		this.fAlta = fAlta;
		this.bloqueo = bloqueo;
	}

	public PrcontraPK getPrcontraPK() {
		return prcontraPK;
	}

	public void setPrcontraPK(PrcontraPK prcontraPK) {
		this.prcontraPK = prcontraPK;
	}

	public BigInteger getAnno() {
		return anno;
	}

	public void setAnno(BigInteger anno) {
		this.anno = anno;
	}

//	public String getNif() {
//		return nif;
//	}
//
//	public void setNif(String nif) {
//		this.nif = nif;
//	}

	public BigInteger getPrssem() {
		return prssem;
	}

	public void setPrssem(BigInteger prssem) {
		this.prssem = prssem;
	}

	public BigInteger getNissem() {
		return nissem;
	}

	public void setNissem(BigInteger nissem) {
		this.nissem = nissem;
	}

	public BigInteger getCossem() {
		return cossem;
	}

	public void setCossem(BigInteger cossem) {
		this.cossem = cossem;
	}

	public BigInteger getRegimen() {
		return regimen;
	}

	public void setRegimen(BigInteger regimen) {
		this.regimen = regimen;
	}

	public String getContacto() {
		return contacto;
	}

	public void setContacto(String contacto) {
		this.contacto = contacto;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public String getFax() {
		return fax;
	}

	public void setFax(String fax) {
		this.fax = fax;
	}

	public BigInteger getNutra() {
		return nutra;
	}

	public void setNutra(BigInteger nutra) {
		this.nutra = nutra;
	}

	public BigInteger getNutrat() {
		return nutrat;
	}

	public void setNutrat(BigInteger nutrat) {
		this.nutrat = nutrat;
	}

	public BigInteger getCnae() {
		return cnae;
	}

	public void setCnae(BigInteger cnae) {
		this.cnae = cnae;
	}

	public BigInteger getGremp() {
		return gremp;
	}

	public void setGremp(BigInteger gremp) {
		this.gremp = gremp;
	}

	public Character getA1() {
		return a1;
	}

	public void setA1(Character a1) {
		this.a1 = a1;
	}

	public Date getFAlta() {
		return fAlta;
	}

	public void setFAlta(Date fAlta) {
		this.fAlta = fAlta;
	}

	public Date getFBaja() {
		return fBaja;
	}

	public void setFBaja(Date fBaja) {
		this.fBaja = fBaja;
	}

	public Float getPrecio() {
		return precio;
	}

	public void setPrecio(Float precio) {
		this.precio = precio;
	}

	public BigInteger getMpago() {
		return mpago;
	}

	public void setMpago(BigInteger mpago) {
		this.mpago = mpago;
	}

	public BigInteger getPerio() {
		return perio;
	}

	public void setPerio(BigInteger perio) {
		this.perio = perio;
	}

	public String getCtabanco() {
		return ctabanco;
	}

	public void setCtabanco(String ctabanco) {
		this.ctabanco = ctabanco;
	}

	public String getQuienfirma() {
		return quienfirma;
	}

	public void setQuienfirma(String quienfirma) {
		this.quienfirma = quienfirma;
	}

	public String getDnifirma() {
		return dnifirma;
	}

	public void setDnifirma(String dnifirma) {
		this.dnifirma = dnifirma;
	}

	public String getCargofirma() {
		return cargofirma;
	}

	public void setCargofirma(String cargofirma) {
		this.cargofirma = cargofirma;
	}

	public Date getFEscritura() {
		return fEscritura;
	}

	public void setFEscritura(Date fEscritura) {
		this.fEscritura = fEscritura;
	}

	public String getNotario() {
		return notario;
	}

	public void setNotario(String notario) {
		this.notario = notario;
	}

	public String getPoblanot() {
		return poblanot;
	}

	public void setPoblanot(String poblanot) {
		this.poblanot = poblanot;
	}

	public String getProtocolo() {
		return protocolo;
	}

	public void setProtocolo(String protocolo) {
		this.protocolo = protocolo;
	}

	public String getLugarfirma() {
		return lugarfirma;
	}

	public void setLugarfirma(String lugarfirma) {
		this.lugarfirma = lugarfirma;
	}

	public BigInteger getContf() {
		return contf;
	}

	public void setContf(BigInteger contf) {
		this.contf = contf;
	}

	public Character getBloqueo() {
		return bloqueo;
	}

	public void setBloqueo(Character bloqueo) {
		this.bloqueo = bloqueo;
	}

	public Character getFactura() {
		return factura;
	}

	public void setFactura(Character factura) {
		this.factura = factura;
	}

	public String getEnpoder() {
		return enpoder;
	}

	public void setEnpoder(String enpoder) {
		this.enpoder = enpoder;
	}

	public Character getCpar1() {
		return cpar1;
	}

	public void setCpar1(Character cpar1) {
		this.cpar1 = cpar1;
	}

	public Character getCpar2() {
		return cpar2;
	}

	public void setCpar2(Character cpar2) {
		this.cpar2 = cpar2;
	}

	public Character getCpar3() {
		return cpar3;
	}

	public void setCpar3(Character cpar3) {
		this.cpar3 = cpar3;
	}

	public Character getCpar4() {
		return cpar4;
	}

	public void setCpar4(Character cpar4) {
		this.cpar4 = cpar4;
	}

	public Character getCpar5() {
		return cpar5;
	}

	public void setCpar5(Character cpar5) {
		this.cpar5 = cpar5;
	}

	public Character getCpar6() {
		return cpar6;
	}

	public void setCpar6(Character cpar6) {
		this.cpar6 = cpar6;
	}

	public Character getCpar7() {
		return cpar7;
	}

	public void setCpar7(Character cpar7) {
		this.cpar7 = cpar7;
	}

	public Character getCpar8() {
		return cpar8;
	}

	public void setCpar8(Character cpar8) {
		this.cpar8 = cpar8;
	}

	public Character getCpar9() {
		return cpar9;
	}

	public void setCpar9(Character cpar9) {
		this.cpar9 = cpar9;
	}

	public Character getSpp1() {
		return spp1;
	}

	public void setSpp1(Character spp1) {
		this.spp1 = spp1;
	}

	public Character getSpp2() {
		return spp2;
	}

	public void setSpp2(Character spp2) {
		this.spp2 = spp2;
	}

	public Character getSpp3() {
		return spp3;
	}

	public void setSpp3(Character spp3) {
		this.spp3 = spp3;
	}

	public Character getRr() {
		return rr;
	}

	public void setRr(Character rr) {
		this.rr = rr;
	}

	public String getTipoc() {
		return tipoc;
	}

	public void setTipoc(String tipoc) {
		this.tipoc = tipoc;
	}

	public String getPtrab() {
		return ptrab;
	}

	public void setPtrab(String ptrab) {
		this.ptrab = ptrab;
	}

	public String getPcurso() {
		return pcurso;
	}

	public void setPcurso(String pcurso) {
		this.pcurso = pcurso;
	}

	public BigInteger getUsualt() {
		return usualt;
	}

	public void setUsualt(BigInteger usualt) {
		this.usualt = usualt;
	}

	public Date getFUsu() {
		return fUsu;
	}

	public void setFUsu(Date fUsu) {
		this.fUsu = fUsu;
	}

	public Character getFirmado() {
		return firmado;
	}

	public void setFirmado(Character firmado) {
		this.firmado = firmado;
	}

	public Character getVs() {
		return vs;
	}

	public void setVs(Character vs) {
		this.vs = vs;
	}

	public BigInteger getCentra() {
		return centra;
	}

	public void setCentra(BigInteger centra) {
		this.centra = centra;
	}

	public BigInteger getRt() {
		return rt;
	}

	public void setRt(BigInteger rt) {
		this.rt = rt;
	}

	public Character getF1() {
		return f1;
	}

	public void setF1(Character f1) {
		this.f1 = f1;
	}

	public Character getIpc() {
		return ipc;
	}

	public void setIpc(Character ipc) {
		this.ipc = ipc;
	}

	public BigInteger getNr() {
		return nr;
	}

	public void setNr(BigInteger nr) {
		this.nr = nr;
	}

	public BigInteger getCoord() {
		return coord;
	}

	public void setCoord(BigInteger coord) {
		this.coord = coord;
	}

	public String getObserv() {
		return observ;
	}

	public void setObserv(String observ) {
		this.observ = observ;
	}

	public BigInteger getTm() {
		return tm;
	}

	public void setTm(BigInteger tm) {
		this.tm = tm;
	}

	public BigInteger getRe() {
		return re;
	}

	public void setRe(BigInteger re) {
		this.re = re;
	}

	public Character getUmovil() {
		return umovil;
	}

	public void setUmovil(Character umovil) {
		this.umovil = umovil;
	}

	public BigInteger getAds() {
		return ads;
	}

	public void setAds(BigInteger ads) {
		this.ads = ads;
	}

	public Date getFDescarga() {
		return fDescarga;
	}

	public void setFDescarga(Date fDescarga) {
		this.fDescarga = fDescarga;
	}

	public BigInteger getGrupoEmp() {
		return grupoEmp;
	}

	public void setGrupoEmp(BigInteger grupoEmp) {
		this.grupoEmp = grupoEmp;
	}

	public String getObserv1() {
		return observ1;
	}

	public void setObserv1(String observ1) {
		this.observ1 = observ1;
	}

	public String getObserv2() {
		return observ2;
	}

	public void setObserv2(String observ2) {
		this.observ2 = observ2;
	}

	public Date getFLlamada1() {
		return fLlamada1;
	}

	public void setFLlamada1(Date fLlamada1) {
		this.fLlamada1 = fLlamada1;
	}

	public Character getNa1() {
		return na1;
	}

	public void setNa1(Character na1) {
		this.na1 = na1;
	}

	public Character getConst1() {
		return const1;
	}

	public void setConst1(Character const1) {
		this.const1 = const1;
	}

	public Date getFEvr() {
		return fEvr;
	}

	public void setFEvr(Date fEvr) {
		this.fEvr = fEvr;
	}

	public Date getFPlan() {
		return fPlan;
	}

	public void setFPlan(Date fPlan) {
		this.fPlan = fPlan;
	}

	public Date getFMemoria() {
		return fMemoria;
	}

	public void setFMemoria(Date fMemoria) {
		this.fMemoria = fMemoria;
	}

	public Date getFRenove() {
		return fRenove;
	}

	public void setFRenove(Date fRenove) {
		this.fRenove = fRenove;
	}

	public Character getInci() {
		return inci;
	}

	public void setInci(Character inci) {
		this.inci = inci;
	}

	public Date getFEnvPlan() {
		return fEnvPlan;
	}

	public void setFEnvPlan(Date fEnvPlan) {
		this.fEnvPlan = fEnvPlan;
	}

	public Float getA1DocImp() {
		return a1DocImp;
	}

	public void setA1DocImp(Float a1DocImp) {
		this.a1DocImp = a1DocImp;
	}

	public Float getA1DocNtra() {
		return a1DocNtra;
	}

	public void setA1DocNtra(Float a1DocNtra) {
		this.a1DocNtra = a1DocNtra;
	}

	public Float getA1RmImp() {
		return a1RmImp;
	}

	public void setA1RmImp(Float a1RmImp) {
		this.a1RmImp = a1RmImp;
	}

	public Float getA1RmNtra() {
		return a1RmNtra;
	}

	public void setA1RmNtra(Float a1RmNtra) {
		this.a1RmNtra = a1RmNtra;
	}

	public Float getNa1DocImp() {
		return na1DocImp;
	}

	public void setNa1DocImp(Float na1DocImp) {
		this.na1DocImp = na1DocImp;
	}

	public Float getNa1DocNtra() {
		return na1DocNtra;
	}

	public void setNa1DocNtra(Float na1DocNtra) {
		this.na1DocNtra = na1DocNtra;
	}

	public Float getNa1RmImp() {
		return na1RmImp;
	}

	public void setNa1RmImp(Float na1RmImp) {
		this.na1RmImp = na1RmImp;
	}

	public Float getNa1RmNtra() {
		return na1RmNtra;
	}

	public void setNa1RmNtra(Float na1RmNtra) {
		this.na1RmNtra = na1RmNtra;
	}

	public Float getConstDocImp() {
		return constDocImp;
	}

	public void setConstDocImp(Float constDocImp) {
		this.constDocImp = constDocImp;
	}

	public Float getConstDocNtra() {
		return constDocNtra;
	}

	public void setConstDocNtra(Float constDocNtra) {
		this.constDocNtra = constDocNtra;
	}

	public Float getConstRmImp() {
		return constRmImp;
	}

	public void setConstRmImp(Float constRmImp) {
		this.constRmImp = constRmImp;
	}

	public String getConstRmNtra() {
		return constRmNtra;
	}

	public void setConstRmNtra(String constRmNtra) {
		this.constRmNtra = constRmNtra;
	}

	public Character getTienePre() {
		return tienePre;
	}

	public void setTienePre(Character tienePre) {
		this.tienePre = tienePre;
	}

	public Character getAceptaPre() {
		return aceptaPre;
	}

	public void setAceptaPre(Character aceptaPre) {
		this.aceptaPre = aceptaPre;
	}

	public Date getFPre() {
		return fPre;
	}

	public void setFPre(Date fPre) {
		this.fPre = fPre;
	}

	public BigInteger getNhoras() {
		return nhoras;
	}

	public void setNhoras(BigInteger nhoras) {
		this.nhoras = nhoras;
	}

	public Float getChora() {
		return chora;
	}

	public void setChora(Float chora) {
		this.chora = chora;
	}

	public BigInteger getIndUnico() {
		return indUnico;
	}

	public void setIndUnico(BigInteger indUnico) {
		this.indUnico = indUnico;
	}

	public BigInteger getNumeroi() {
		return numeroi;
	}

	public void setNumeroi(BigInteger numeroi) {
		this.numeroi = numeroi;
	}

	public BigInteger getRenovi() {
		return renovi;
	}

	public void setRenovi(BigInteger renovi) {
		this.renovi = renovi;
	}

	public BigInteger getTipoi() {
		return tipoi;
	}

	public void setTipoi(BigInteger tipoi) {
		this.tipoi = tipoi;
	}

	public Float getTarifa() {
		return tarifa;
	}

	public void setTarifa(Float tarifa) {
		this.tarifa = tarifa;
	}

	public Float getIpcPorc() {
		return ipcPorc;
	}

	public void setIpcPorc(Float ipcPorc) {
		this.ipcPorc = ipcPorc;
	}

	public Date getFRecMem() {
		return fRecMem;
	}

	public void setFRecMem(Date fRecMem) {
		this.fRecMem = fRecMem;
	}

	public Date getFRecPlan() {
		return fRecPlan;
	}

	public void setFRecPlan(Date fRecPlan) {
		this.fRecPlan = fRecPlan;
	}

	public Float getIpcImp() {
		return ipcImp;
	}

	public void setIpcImp(Float ipcImp) {
		this.ipcImp = ipcImp;
	}

	public Float getUmovilImp() {
		return umovilImp;
	}

	public void setUmovilImp(Float umovilImp) {
		this.umovilImp = umovilImp;
	}

	public String getQuienfirma2() {
		return quienfirma2;
	}

	public void setQuienfirma2(String quienfirma2) {
		this.quienfirma2 = quienfirma2;
	}

	public String getDnifirma2() {
		return dnifirma2;
	}

	public void setDnifirma2(String dnifirma2) {
		this.dnifirma2 = dnifirma2;
	}

	public String getCargofirma2() {
		return cargofirma2;
	}

	public void setCargofirma2(String cargofirma2) {
		this.cargofirma2 = cargofirma2;
	}

	public String getDomicEnv() {
		return domicEnv;
	}

	public void setDomicEnv(String domicEnv) {
		this.domicEnv = domicEnv;
	}

	public BigInteger getProviEnv() {
		return proviEnv;
	}

	public void setProviEnv(BigInteger proviEnv) {
		this.proviEnv = proviEnv;
	}

	public BigInteger getPoblaEnv() {
		return poblaEnv;
	}

	public void setPoblaEnv(BigInteger poblaEnv) {
		this.poblaEnv = poblaEnv;
	}

	public BigInteger getDispoEnv() {
		return dispoEnv;
	}

	public void setDispoEnv(BigInteger dispoEnv) {
		this.dispoEnv = dispoEnv;
	}

	public BigInteger getMutOrigen() {
		return mutOrigen;
	}

	public void setMutOrigen(BigInteger mutOrigen) {
		this.mutOrigen = mutOrigen;
	}

	public String getCodConcierto() {
		return codConcierto;
	}

	public void setCodConcierto(String codConcierto) {
		this.codConcierto = codConcierto;
	}

	public String getNPedido() {
		return nPedido;
	}

	public void setNPedido(String nPedido) {
		this.nPedido = nPedido;
	}

	public BigInteger getDFactura() {
		return dFactura;
	}

	public void setDFactura(BigInteger dFactura) {
		this.dFactura = dFactura;
	}

	public String getDVencimiento() {
		return dVencimiento;
	}

	public void setDVencimiento(String dVencimiento) {
		this.dVencimiento = dVencimiento;
	}

	@Override
	public int hashCode() {
		int hash = 0;
		hash += ( prcontraPK != null ? prcontraPK.hashCode() : 0 );
		return hash;
	}

	@Override
	public boolean equals(Object object) {
		if ( !( object instanceof Prcontra ) ) {
			return false;
		}
		Prcontra other = ( Prcontra ) object;
		if ( this.prcontraPK != other.prcontraPK
				&& ( this.prcontraPK == null || !this.prcontraPK
				.equals( other.prcontraPK ) ) ) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "org.kyrian.entity.muvale.Prcontra[prcontraPK=" + prcontraPK
				+ "]";
	}
	//START-ENTITY-MOD

	public String getTextoMpago() {
		return textoMpago;
	}

	public void setTextoMpago(String textoMpago) {
		this.textoMpago = textoMpago;
	}

	public Integer getTotalAfiliados() {
		return totalAfiliados;
	}

	public void setTotalAfiliados(Integer totalAfiliados) {
		this.totalAfiliados = totalAfiliados;
	}

	public String getModo() {
		return modo;
	}

	public void setModo(String modo) {
		this.modo = modo;
	}

	public Integer getImpagado() {
		return impagado;
	}

	public void setImpagado(Integer impagado) {
		this.impagado = impagado;
	}

	public Long getNumReconRealizados() {
		return numReconRealizados;
	}

	public void setNumReconRealizados(Long numReconRealizados) {
		this.numReconRealizados = numReconRealizados;
	}

	public Long getNumReconPrevistos() {
		return numReconPrevistos;
	}

	public void setNumReconPrevistos(Long numReconPrevistos) {
		this.numReconPrevistos = numReconPrevistos;
	}

	public BigInteger getConNrmRh() {
		return conNrmRh;
	}

	public BigInteger getConNrmRt() {
		return conNrmRt;
	}

	public void setConNrmRh(BigInteger conNrmRh) {
		this.conNrmRh = conNrmRh;
	}

	public void setConNrmRt(BigInteger conNrmRt) {
		this.conNrmRt = conNrmRt;
	}

	public Float getConPrecioRmExceso() {
		return conPrecioRmExceso;
	}

	public BigInteger getConNtraDocRh() {
		return conNtraDocRh;
	}

	public Float getConImpDocRhigienico() {
		return conImpDocRhigienico;
	}

	public Float getConImprmRhigienico() {
		return conImprmRhigienico;
	}

	public BigInteger getConNtraDocRt() {
		return conNtraDocRt;
	}

	public Float getConImpdocRTipologias() {
		return conImpdocRTipologias;
	}

	public Float getConImprmRtipologias() {
		return conImprmRtipologias;
	}

	public Float getConImpUmovil() {
		return conImpUmovil;
	}

	public Float getConPrecioSme() {
		return conPrecioSme;
	}

	public void setConPrecioRmExceso(Float conPrecioRmExceso) {
		this.conPrecioRmExceso = conPrecioRmExceso;
	}

	public void setConNtraDocRh(BigInteger conNtraDocRh) {
		this.conNtraDocRh = conNtraDocRh;
	}

	public void setConImpDocRhigienico(Float conImpDocRhigienico) {
		this.conImpDocRhigienico = conImpDocRhigienico;
	}

	public void setConImprmRhigienico(Float conImprmRhigienico) {
		this.conImprmRhigienico = conImprmRhigienico;
	}

	public void setConNtraDocRt(BigInteger conNtraDocRt) {
		this.conNtraDocRt = conNtraDocRt;
	}

	public void setConImpdocRTipologias(Float conImpdocRTipologias) {
		this.conImpdocRTipologias = conImpdocRTipologias;
	}

	public void setConImprmRtipologias(Float conImprmRtipologias) {
		this.conImprmRtipologias = conImprmRtipologias;
	}

	public void setConImpUmovil(Float conImpUmovil) {
		this.conImpUmovil = conImpUmovil;
	}

	public void setConPrecioSme(Float conPrecioSme) {
		this.conPrecioSme = conPrecioSme;
	}

	public BigInteger getDoc() {
		return doc;
	}

	public String getNif() {
		return nif;
	}

	public void setDoc(BigInteger doc) {
		this.doc = doc;
	}

	public void setNif(String nif) {
		this.nif = nif;
	}
}


