package xbrl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

public class DownloadStockSplits {

	// rerun from stockdata symbols current list.

	// get historical split history for each symbol sought. Update tickers
	// list from stockdata.

	public static String splitServer = "https://www.splithistory.com/?symbol=";
	public static String dividendServer = "https://www.dividendchannel.com/symbol/";

	public static String[] tickers = { "A", "AA", "AAI", "AAII", "AAMRQ", "AAN", "AAON", "AAP", "AAPL", "AAS", "AAUK",
			"AAUKY", "AB", "ABC", "ABCB", "ABCFD", "ABCFF", "ABCO", "ABEV", "ABFOF", "ABGX", "ABK", "ABLT", "ABM",
			"ABMD", "ABOV", "ABRTY", "ABT", "ABV", "ABVA", "ABVT", "ABX", "ACAI", "ACAP", "ACAT", "ACBA", "ACCP",
			"ACCPD", "ACDO", "ACE", "ACET", "ACFN", "ACGL", "ACH", "ACHC", "ACI", "ACIW", "ACKDF", "ACLI", "ACLLF",
			"ACLNF", "ACLS", "ACMR", "ACNB", "ACO", "ACOSF", "ACS", "ACSAF", "ACTRF", "ACTU", "ACV", "ACVC", "ACXM",
			"AD", "ADAP", "ADBE", "ADCT", "ADDYY", "ADES", "ADI", "ADIC", "ADK", "ADLI", "ADM", "ADMS", "ADO", "ADP",
			"ADPI", "ADPT", "ADRA", "ADRD", "ADRE", "ADRU", "ADRX", "ADSK", "ADTN", "ADVC", "ADVNA", "ADVNB", "ADVP",
			"ADVS", "ADX", "AE", "AEG", "AEO", "AEOS", "AEPI", "AES", "AESYY", "AET", "AEYIF", "AF", "AFAM", "AFFX",
			"AFG", "AFL", "AFOP", "AFP", "AFSI", "AGC", "AGCO", "AGD", "AGEM", "AGESY", "AGII", "AGIL", "AGL", "AGM",
			"AGMJF", "AGN", "AGP", "AGPPY", "AGPYF", "AGQ", "AGU", "AGYS", "AHAA", "AHCHY", "AHONY", "AI", "AIB",
			"AIBYY", "AIFLY", "AIG", "AIN", "AINN", "AIQ", "AIQUF", "AIQUY", "AIR", "AIRM", "AIT", "AJG", "AJINY",
			"AJRD", "AKBTY", "AKKVF", "AKS", "AKZOY", "ALB", "ALBY", "ALC", "ALCO", "ALDA", "ALE", "ALFA", "ALFFF",
			"ALGX", "ALK", "ALL", "ALLB", "ALLR", "ALLY", "ALMCD", "ALNXF", "ALNXY", "ALO", "ALOG", "ALOT", "ALPMF",
			"ALPMY", "ALR", "ALRC", "ALRS", "ALSI", "ALT", "ALTR", "ALTV", "ALXN", "AM", "AMAT", "AMBK", "AMCC",
			"AMCRY", "AMD", "AME", "AMED", "AMEH", "AMFC", "AMG", "AMGN", "AMHC", "AMIN", "AMK", "AMKAF", "AMKBF",
			"AMMD", "AMN", "AMNB", "AMNF", "AMOV", "AMR", "AMRB", "AMSG", "AMSWA", "AMSYF", "AMTD", "AMWD", "AMX",
			"AMZN", "AN", "ANCUF", "ANCX", "ANDE", "ANEN", "ANF", "ANFI", "ANK", "ANLY", "ANN", "ANNB", "ANPI", "ANSI",
			"ANSS", "ANST", "ANTH", "ANTM", "ANZBY", "AOC", "AOD", "AOI", "AOL", "AON", "AOS", "APA", "APAC", "APAGF",
			"APC", "APCC", "APD", "APEX", "APH", "APHB", "APNI", "APOG", "APOL", "APPB", "APPX", "APWR", "APYI", "AQFH",
			"AQQ", "ARBA", "ARCAY", "ARCO", "ARCW", "ARD", "AREA", "ARECD", "ARG", "ARGKF", "ARGT", "ARKAF", "ARL",
			"ARLP", "ARM", "ARMH", "ARMHY", "ARNC", "ARO", "AROW", "ARR", "ARRO", "ARROB", "ARSLD", "ARTNA", "ARTNB",
			"ARTX", "ARW", "ARXX", "ASA", "ASAZY", "ASB", "ASBC", "ASBI", "ASCA", "ASD", "ASDOF", "ASDV", "ASEJF",
			"ASF", "ASFC", "ASFI", "ASGN", "ASGR", "ASH", "ASMI", "ASML", "ASNA", "ASO", "ASRV", "ASTE", "ASTSF",
			"ASVI", "ASWAY", "ASX", "ASXSF", "ASYT", "ATA", "ATASY", "ATCS", "ATE", "ATEA", "ATHM", "ATK", "ATLCY",
			"ATLKY", "ATLO", "ATLS", "ATML", "ATNI", "ATO", "ATR", "ATRI", "ATRO", "ATROB", "ATTC", "ATU", "ATVI",
			"ATW", "AUBN", "AUDC", "AUGR", "AUKNY", "AUKUF", "AUO", "AUTN", "AVA", "AVD", "AVIA", "AVNR", "AVP", "AVT",
			"AVTC", "AVX", "AVY", "AVZ", "AWH", "AWR", "AWS", "AXA", "AXAHY", "AXE", "AXF", "AXFOF", "AXFOY", "AXJV",
			"AXLL", "AXP", "AXR", "AXYS", "AYALY", "AYE", "AZA", "AZN", "AZO", "AZPN", "AZZ", "B", "BA", "BAA", "BAC",
			"BACPY", "BADFF", "BAFYY", "BAK", "BALT", "BAM", "BAMM", "BANF", "BANR", "BAOB", "BAP", "BARAD", "BARZ",
			"BASFY", "BAX", "BAYK", "BBBY", "BBCN", "BBD", "BBDO", "BBH", "BBRC", "BBRY", "BBSI", "BBSW", "BBT", "BBVA",
			"BBVXF", "BBX", "BBY", "BC", "BCA", "BCAR", "BCBHF", "BCBP", "BCDRF", "BCE", "BCHS", "BCOR", "BCP", "BCPC",
			"BCR", "BCS", "BCST", "BCXQL", "BDC", "BDGE", "BDIMF", "BDJI", "BDL", "BDMS", "BDULF", "BDUUF", "BDVSF",
			"BDVSY", "BDX", "BDY", "BE", "BEAM", "BEAS", "BEAV", "BEBE", "BEC", "BECN", "BEIQ", "BEL", "BELFA", "BELFB",
			"BELM", "BEM", "BEN", "BEN`", "BEOB", "BER", "BERK", "BETR", "BEVFF", "BEZ", "BF-A", "BF-B", "BFAM", "BFCF",
			"BFCFB", "BFCI", "BFR", "BFRE", "BGC", "BGEN", "BGG", "BGZ", "BHB", "BHE", "BHP", "BIB", "BIDU", "BIG",
			"BIO", "BIOL", "BIP", "BIRT", "BIS", "BJ", "BJS", "BJURF", "BK", "BKE", "BKH", "BKI", "BKJAY", "BKNIY",
			"BKNW", "BKRKF", "BKS", "BKSC", "BKST", "BKYF", "BLL", "BLLI", "BLMK", "BLSC", "BLTA", "BLUD", "BMBN",
			"BMC", "BMET", "BMHC", "BMI", "BMNM", "BMO", "BMRC", "BMS", "BMTC", "BMY", "BNCN", "BNHC", "BNHN", "BNKXF",
			"BNN", "BNO", "BNS", "BNSO", "BNTGF", "BOBJ", "BOCH", "BOH", "BOIL", "BOIVF", "BOKF", "BOLT", "BOMK",
			"BOOM", "BOTA", "BOTJ", "BOYL", "BP", "BPA", "BPAC", "BPL", "BPMLF", "BPO", "BPOP", "BR", "BRBW", "BRC",
			"BRCD", "BRCM", "BRE", "BRER", "BRFS", "BRGYY", "BRID", "BRL", "BRLI", "BRN", "BRO", "BRPIX", "BRSGD",
			"BRT", "BRVVD", "BRVVF", "BRY", "BRZU", "BSAC", "BSBN", "BSET", "BSF", "BSRR", "BSTC", "BSTN", "BSX",
			"BSYBY", "BSYS", "BT", "BTBIF", "BTFRF", "BTH", "BTI", "BTO", "BTOF", "BTU", "BTX", "BUCY", "BUD", "BURCA",
			"BUSE", "BUTL", "BVEW", "BVF", "BVN", "BVSN", "BWA", "BWAY", "BWE", "BWINA", "BWINB", "BWL-A", "BWLD",
			"BWS", "BWX", "BXC", "BXE", "BXM", "BXMT", "BXS", "BYBK", "BYDDY", "BYFC", "BYI", "BYLK", "BYS", "BZH",
			"BZLFY", "BZQ", "C", "CA", "CAA", "CAC", "CACB", "CACC", "CACH", "CACI", "CAE", "CAFI", "CAG", "CAGAQ",
			"CAGC", "CAH", "CAIXY", "CAJ", "CAK", "CAKE", "CAL", "CALCQ", "CALL", "CALM", "CALVD", "CALVF", "CAM",
			"CAPX", "CAR", "CARO", "CAS", "CASB", "CASS", "CASY", "CAT", "CATC", "CATO", "CATSD", "CATY", "CAW", "CB",
			"CBA", "CBAF", "CBAN", "CBB", "CBBC", "CBD", "CBE", "CBFV", "CBG", "CBH", "CBI", "CBIN", "CBK", "CBKM",
			"CBL", "CBM", "CBNC", "CBR", "CBRL", "CBSH", "CBSS", "CBT", "CBTC", "CBU", "CBUK", "CBXC", "CC", "CCBG",
			"CCBI", "CCBL", "CCBN", "CCBT", "CCC", "CCCL", "CCDBF", "CCE", "CCF", "CCFH", "CCH", "CCJ", "CCK", "CCL",
			"CCLAY", "CCM", "CCNE", "CCOW", "CCU", "CCUR", "CCYY", "CDBK", "CDI", "CDIR", "CDIS", "CDNS", "CDR",
			"CDSCY", "CDT", "CDUAF", "CDWC", "CDXC", "CEA", "CEB", "CEC", "CECB", "CECO", "CECX", "CEDC", "CEE", "CEFC",
			"CEFT", "CEG", "CELG", "CELL", "CEN", "CENI", "CEQP", "CERN", "CERT", "CESJF", "CETV", "CF", "CFBC", "CFC",
			"CFCB", "CFCP", "CFFC", "CFFI", "CFFN", "CFI", "CFIC", "CFICD", "CFIS", "CFNB", "CFP", "CFR", "CFWFF",
			"CGG", "CGI", "CGL-A", "CGN", "CGNX", "CGO", "CGX", "CH", "CHAP", "CHBS", "CHCO", "CHCR", "CHCS", "CHD",
			"CHDN", "CHDX", "CHE", "CHEXD", "CHFC", "CHH", "CHIM", "CHK", "CHKP", "CHL", "CHMP", "CHNB", "CHP", "CHRS",
			"CHRW", "CHS", "CHT", "CHTT", "CHUX", "CHV", "CHYFF", "CHZ", "CHZS", "CI", "CIA", "CIBEY", "CIBN", "CICHY",
			"CIE", "CIEN", "CIG", "CIGI", "CIITD", "CIM", "CINF", "CIOXY", "CIWV", "CIZN", "CJBK", "CJHBQ", "CJR",
			"CJREF", "CKEC", "CKFB", "CKH", "CKP", "CL", "CLAC", "CLB", "CLBC", "CLBK", "CLBS", "CLC", "CLCGY", "CLCT",
			"CLDA", "CLDB", "CLDN", "CLE", "CLF", "CLFC", "CLGX", "CLH", "CLM", "CLR", "CLS", "CLSN", "CLW", "CLX",
			"CLZR", "CMA", "CMAKY", "CMB", "CMC", "CMCSA", "CMCSK", "CMD", "CMDL", "CMDXF", "CME", "CMGGF", "CMGI",
			"CMI", "CMN", "CMO", "CMOH", "CMOS", "CMPGD", "CMPGF", "CMPGY", "CMRC", "CMRG", "CMRL", "CMRO", "CMSB",
			"CMTB", "CMTL", "CMTN", "CMTV", "CMUY", "CMVT", "CNA", "CNAF", "CNBC", "CNBL", "CNC", "CNCO", "CNCX",
			"CNET", "CNI", "CNIG", "CNL", "CNMD", "CNOB", "CNP", "CNQ", "CNR", "CNT", "CNTE", "CNTF", "CNTL", "CNW",
			"CNX", "CNXN", "CNXS", "CNXT", "COB", "COBH", "COBJF", "COBZ", "COCO", "COF", "COFS", "COG", "COGN", "COH",
			"COHM", "COHR", "COHU", "COIHF", "COLB", "COLM", "COMM", "COMMF", "COMR", "COMS", "COO", "COOL", "COOP",
			"COP", "COPX", "CORB", "CORE", "CORR", "CORS", "COST", "COSWF", "COT", "COTQD", "COVD", "COWN", "COX", "CP",
			"CPAH", "CPB", "CPBLF", "CPE", "CPF", "CPG", "CPK", "CPKF", "CPKI", "CPL", "CPN", "CPNBF", "CPNO", "CPO",
			"CPRT", "CPS", "CPSS", "CPTH", "CPTL", "CPTP", "CPWM", "CPWR", "CQB", "CR", "CRBJF", "CRC", "CRD-A",
			"CRD-B", "CRDN", "CREAF", "CRED", "CREE", "CRESY", "CRF", "CRGN", "CRH", "CRHCY", "CRI", "CRK", "CRM",
			"CRMZ", "CROX", "CRPT", "CRR", "CRRB", "CRRC", "CRS", "CRTQ", "CRVL", "CRXM", "CRY", "CRZBY", "CRZHD",
			"CSBQ", "CSC", "CSCO", "CSFL", "CSG", "CSGKF", "CSGS", "CSH", "CSL", "CSLMF", "CSM", "CSNT", "CSPI",
			"CSRLF", "CSS", "CSUAY", "CSVI", "CSWC", "CSX", "CTAC", "CTAS", "CTB", "CTBI", "CTBK", "CTBP", "CTC",
			"CTCI", "CTG", "CTGX", "CTHR", "CTL", "CTMMA", "CTMMB", "CTOT", "CTRN", "CTRP", "CTRX", "CTS", "CTSH",
			"CTTAY", "CTWS", "CTX", "CTXRD", "CTXS", "CTZR", "CUB", "CUBN", "CURE", "CUUCF", "CUZ", "CVB", "CVBF",
			"CVC", "CVCO", "CVCY", "CVD", "CVH", "CVIA", "CVLY", "CVO", "CVOL", "CVS", "CVX", "CW", "CWBB", "CWBC",
			"CWCO", "CWH", "CWLZ", "CWT", "CWTR", "CWXZF", "CX", "CXM", "CXR", "CXW", "CY", "CYBE", "CYBK", "CYCL",
			"CYH", "CYRBY", "CYSV", "CYT", "CYTC", "CYTR", "CZBC", "CZFC", "CZFS", "CZNC", "CZNL", "D", "DA", "DADE",
			"DAKT", "DANOY", "DAR", "DARA", "DASTF", "DASTY", "DATA", "DAWK", "DBD", "DBIN", "DBRN", "DCAI", "DCBK",
			"DCI", "DCLK", "DCM", "DCMI", "DCO", "DCOM", "DCR", "DCS", "DCT", "DCTH", "DCTM", "DD", "DDD", "DDE", "DDM",
			"DDR", "DDRX", "DDS", "DE", "DECK", "DEER", "DEG", "DELL", "DEST", "DF", "DFG", "DFXI", "DFZ", "DG", "DGAS",
			"DGICA", "DGICB", "DGX", "DHG", "DHI", "DHIL", "DHR", "DHT", "DIGM", "DII", "DIIBF", "DIMC", "DIN", "DINIQ",
			"DIOD", "DIRV", "DIS", "DISCA", "DISCK", "DISH", "DIT", "DITC", "DIYS", "DKS", "DKWD", "DLA", "DLK", "DLLR",
			"DLMAF", "DLTR", "DLX", "DMAS", "DNA", "DNB", "DNBF", "DNEX", "DNHBY", "DNI", "DNN", "DNPLY", "DNR",
			"DNZOY", "DO", "DORL", "DORM", "DOV", "DOW", "DOY", "DP", "DPK", "DPL", "DPRTF", "DRAM", "DRCO", "DRD",
			"DRE", "DRI", "DRIP", "DRL", "DRN", "DRQ", "DRTE", "DRV", "DS", "DSCI", "DSCO", "DSEEY", "DSETD", "DSPG",
			"DST", "DSU", "DSV", "DSW", "DSWL", "DTEGF", "DTM", "DTRX", "DUC", "DUG", "DUK", "DUOT", "DUOTD", "DUST",
			"DV", "DVA", "DVD", "DVLN", "DVN", "DW", "DWAHY", "DWAQ", "DWL", "DWNX", "DWSN", "DX", "DXD", "DXJR",
			"DXLG", "DXPE", "DXSPF", "DY", "DYN", "DYNIQ", "DYNT", "DZSI", "E", "EA", "EAC", "EADSY", "EAII", "EASI",
			"EAT", "EBAY", "EBF", "EBIX", "EBMT", "EBR", "EBRPY", "EBSB", "EBTC", "ECA", "ECC", "ECL", "ECOL", "ECPCY",
			"ED", "EDC", "EDCI", "EDEN", "EDMC", "EDR", "EDU", "EDUC", "EDZ", "EEEI", "EEG", "EEI", "EEM", "EEO", "EEP",
			"EEQ", "EES", "EEV", "EFA", "EFN", "EFSI", "EFU", "EFX", "EGBN", "EGLS", "EGN", "EGP", "EGPT", "EGRP",
			"EIDSY", "EIX", "EJPRY", "EL", "ELAB", "ELCI", "ELCMF", "ELEZF", "ELK", "ELN", "ELNK", "ELNT", "ELON",
			"ELRC", "ELRNF", "ELS", "ELSE", "ELUXY", "ELV", "ELY", "EMAN", "EMC", "EMCI", "EME", "EMF", "EMITF", "EMKR",
			"EML", "EMLAF", "EMLX", "EMN", "EMR", "EMT", "EMULX", "ENB", "ENBRF", "END", "ENE", "ENEVY", "ENGA",
			"ENGSY", "ENI", "ENL", "ENLC", "ENLK", "ENR", "ENSG", "ENSI", "ENZ", "EOCC", "EOG", "EON", "EONGY", "EP",
			"EPAX", "EPD", "EPIQ", "EPNY", "EPP", "EPV", "EQC", "EQR", "EQS", "EQT", "EQU", "ERDBF", "ERES", "ERGO",
			"ERIC", "ERICY", "ERIE", "ERTS", "ERUS", "ERY", "ESBF", "ESBK", "ESCA", "ESCC", "ESDID", "ESE", "ESEA",
			"ESI", "ESIO", "ESL", "ESND", "ESP", "ESRX", "ESV", "ESYJY", "ETC", "ETE", "ETEC", "ETFC", "ETH", "ETN",
			"ETP", "EUSA", "EV", "EVBN", "EVG", "EVNVY", "EVOL", "EVRC", "EVRT", "EW", "EWBC", "EWI", "EWJ", "EWM",
			"EWS", "EWT", "EWU", "EWV", "EXAC", "EXAP", "EXAR", "EXBD", "EXC", "EXDS", "EXJF", "EXPD", "EXPE", "EXPEW",
			"EXPO", "EXXA", "EZA", "EZM", "EZPW", "EZRHY", "EZU", "F", "FAB", "FABP", "FAC", "FAME", "FANUY", "FARM",
			"FAS", "FAST", "FAZ", "FBC", "FBCI", "FBIZ", "FBMI", "FBMS", "FBMT", "FBNC", "FBNI", "FBNW", "FBP", "FBR",
			"FBRC", "FBSI", "FBSS", "FBTC", "FBTT", "FBVA", "FCAP", "FCAU", "FCBC", "FCBI", "FCCO", "FCCY", "FCE-A",
			"FCE-B", "FCEA", "FCEB", "FCEL", "FCEN", "FCF", "FCFS", "FCG", "FCL", "FCN", "FCPT", "FCSX", "FCX", "FCZA",
			"FD", "FDBC", "FDBH", "FDC", "FDG", "FDHG", "FDLB", "FDO", "FDRA", "FDRY", "FDS", "FDX", "FEIM", "FELE",
			"FESNF", "FETM", "FEXXD", "FFBC", "FFBH", "FFCH", "FFED", "FFEX", "FFFL", "FFG", "FFIC", "FFIN", "FFIS",
			"FFIV", "FFKT", "FFKY", "FFLC", "FFSL", "FFWC", "FFWM", "FFYF", "FGBI", "FGFH", "FGHC", "FHCC", "FHN",
			"FHNIY", "FHRX", "FHY", "FI", "FIA", "FIATY", "FIC", "FICO", "FIF", "FII", "FINB", "FINL", "FINU", "FINZ",
			"FIS", "FISB", "FISV", "FISVC", "FITB", "FIZZ", "FKKY", "FKL", "FKYS", "FL", "FLEX", "FLGS", "FLI", "FLIC",
			"FLIR", "FLO", "FLR", "FLS", "FLSH", "FMAO", "FMAR", "FMBH", "FMBI", "FMBM", "FMC", "FMCB", "FMCC", "FMCN",
			"FMD", "FMER", "FMFC", "FMFG", "FMFP", "FMMH", "FMNB", "FMOO", "FMS", "FMSB", "FMX", "FMYRD", "FNB", "FNBF",
			"FNBG", "FNCB", "FNF", "FNFG", "FNHC", "FNHM", "FNLC", "FNMA", "FNMFO", "FNP", "FNSC", "FO", "FOBB", "FOE",
			"FOFN", "FON", "FOOT", "FOSL", "FOTO", "FOX", "FOXA", "FPBF", "FPBS", "FPIC", "FPL", "FPU", "FPVD", "FPVDD",
			"FQVLF", "FRAF", "FRBK", "FRC", "FRD", "FRED", "FREE", "FREVS", "FRGB", "FRK", "FRME", "FRNT", "FRO",
			"FRSB", "FRTSF", "FRX", "FSBK", "FSCR", "FSII", "FSLA", "FSNM", "FSNUY", "FSR", "FSRV", "FSS", "FST",
			"FTBK", "FTDL", "FTHR", "FTI", "FTK", "FTNT", "FTO", "FTR", "FTSM", "FUELQ", "FUJHY", "FUJIY", "FUL",
			"FULL", "FULT", "FUN", "FUNC", "FUR", "FVB", "FWLT", "FWRD", "FWV", "FXCM", "FXI", "FXNC", "FXP", "GAB",
			"GABA", "GABC", "GADZ", "GALT", "GALXF", "GAMM", "GANS", "GAS", "GASK", "GASL", "GASX", "GATX", "GBBK",
			"GBCI", "GBFL", "GBIX", "GBL", "GBLX", "GBNK", "GBSND", "GCBC", "GCCO", "GCH", "GCI", "GD", "GDI", "GDJJ",
			"GDT", "GDW", "GDXJ", "GDXS", "GDXX", "GE", "GEAPO", "GEAPP", "GEARD", "GEC", "GECR", "GEF", "GEF-B",
			"GEHL", "GEN", "GENC", "GENI", "GEO", "GEOS", "GERS", "GERSD", "GES", "GET", "GEX", "GFA", "GFC", "GFED",
			"GFF", "GFIG", "GG", "GGB", "GGE", "GGG", "GGGG", "GGP", "GHC", "GHM", "GIB", "GIFI", "GIGM", "GIII",
			"GIKLY", "GIL", "GILD", "GIS", "GISX", "GK", "GKSR", "GLBS", "GLBZ", "GLCH", "GLDX", "GLF", "GLL", "GLLA",
			"GLPW", "GLT", "GLW", "GLYT", "GM", "GMCR", "GMDTF", "GME", "GMETP", "GMH", "GMRK", "GMST", "GMT", "GNBF",
			"GNET", "GNI", "GNK", "GNL", "GNTX", "GNWR", "GOGL", "GOGO", "GOL", "GOLD", "GOOG", "GOOGL", "GP", "GPC",
			"GPK", "GPN", "GPRO", "GPS", "GPT", "GPTX", "GPX", "GR", "GRA", "GRAN", "GRBS", "GRC", "GRCI", "GRDN",
			"GRF", "GRFS", "GRMN", "GROW", "GRRB", "GSBC", "GSCB", "GSK", "GSOF", "GSPN", "GSR", "GT", "GTIV", "GTK",
			"GTLL", "GTMAY", "GTN-A", "GTSG", "GTW", "GUMRY", "GUSH", "GUZBY", "GVA", "GWLLY", "GWR", "GWW", "GXG",
			"GXP", "GY", "GYMB", "GYRO", "GZPHF", "HAE", "HAF", "HAFC", "HAIN", "HAL", "HAMP", "HANS", "HAR", "HARB",
			"HARL", "HAS", "HAV", "HAWPF", "HBAN", "HBHC", "HBI", "HBKA", "HBKS", "HBNC", "HBNK", "HBOOY", "HBSI", "HC",
			"HCBK", "HCBN", "HCC", "HCMLY", "HCP", "HCSG", "HCXLF", "HD", "HDB", "HDI", "HDNG", "HE", "HEI", "HEI-A",
			"HEINY", "HELE", "HENGY", "HENKY", "HENOY", "HEOP", "HEP", "HERZ", "HES", "HFBA", "HFBC", "HFBK", "HFC",
			"HFFC", "HGIC", "HGSI", "HH", "HHGP", "HHS", "HHULF", "HIBB", "HICKA", "HIFS", "HIG", "HIH", "HINKY",
			"HIST", "HITK", "HKFI", "HLAN", "HLDCY", "HLDVF", "HLF", "HLIT", "HLNFF", "HLPPY", "HLS", "HLX", "HMA",
			"HMC", "HMH", "HMK", "HMN", "HMNF", "HMNY", "HMPR", "HMST", "HMSY", "HNBC", "HNHPF", "HNI", "HNNA", "HNNMY",
			"HNP", "HNT", "HNZ", "HOC", "HOFD", "HOFT", "HOG", "HOKCF", "HOKCY", "HOLX", "HOMB", "HOMF", "HON", "HONT",
			"HOPE", "HORC", "HOT", "HOTT", "HOV", "HOWWF", "HP", "HPHTF", "HPQ", "HQCL", "HRB", "HRBK", "HRBT", "HRC",
			"HRELY", "HRG", "HRH", "HRI", "HRL", "HRLY", "HRS", "HRZB", "HSA", "HSC", "HSH", "HSIC", "HSKA", "HSON",
			"HSP", "HST", "HSY", "HT", "HTA", "HTBK", "HTCO", "HTHIY", "HTLD", "HTLF", "HTR", "HUB-B", "HUBB", "HUBG",
			"HUM", "HUSKF", "HUWHY", "HVB", "HVT", "HVT-A", "HWALD", "HWAY", "HWBK", "HWDY", "HWEN", "HWG", "HWP",
			"HXL", "HXPLF", "HYB", "HYMLF", "HYSL", "IAC", "IACI", "IALB", "IART", "IAU", "IBBI", "IBCC", "IBCD",
			"IBCE", "IBCP", "IBDB", "IBDC", "IBDD", "IBDF", "IBDH", "IBDJ", "IBDK", "IBDL", "IBDM", "IBDN", "IBDO",
			"IBDP", "IBDQ", "IBDRY", "IBDSF", "IBI", "IBKC", "IBM", "IBME", "IBMF", "IBN", "IBOC", "ICA", "ICAGY",
			"ICE", "ICEIF", "ICF", "ICHGF", "ICII", "ICLR", "ICUI", "ICVHF", "ID", "IDCBY", "IDEXY", "IDI", "IDN",
			"IDPH", "IDSA", "IDT", "IDTI", "IDX", "IDXX", "IEP", "IEV", "IEX", "IFCJ", "IFDG", "IFF", "IFIN", "IFMI",
			"IFNY", "IFO", "IFSB", "IFSIA", "IGE", "IGT", "IGTE", "IHC", "IHG", "IHOP", "IIBK", "IIIN", "IIN", "IIVI",
			"IJH", "IJJ", "IJK", "IJR", "IJS", "IJT", "IKAN", "IKNX", "IKSGY", "ILF", "ILI", "ILMN", "IMAX", "IMCL",
			"IMDC", "IMGC", "IMH", "IMIAY", "IMNP", "IMNX", "IMO", "IMOS", "IMPH", "IMPUY", "INBK", "INCB", "INCX",
			"INCY", "INDL", "INFY", "ING", "INGR", "INHL", "INHO", "INKT", "INMD", "INO", "INOD", "INSP", "INSS",
			"INSY", "INT", "INTC", "INTU", "INTV", "IOSP", "IOT", "IP", "IPAR", "IPG", "IPL", "IPSW", "IR", "IRE",
			"IRF", "IRM", "IRS", "IRW", "ISDR", "ISH", "ISI", "ISSC", "ISSX", "ISTB", "ISUZF", "ISUZY", "ISYS", "IT",
			"ITC", "ITEX", "ITF", "ITG", "ITM", "ITN", "ITOT", "ITPOF", "ITRU", "ITT", "ITU", "ITUB", "ITUS", "ITW",
			"ITWO", "ITYC", "IUSB", "IUSG", "IUSV", "IVC", "IVGN", "IVNYY", "IVX", "IVZ", "IWM", "IWN", "IWOV", "IWP",
			"IWS", "IX", "IXC", "IYCOY", "IYE", "IYR", "JACK", "JACO", "JAH", "JAKK", "JASO", "JBHT", "JBL", "JBLU",
			"JBOH", "JBX", "JCI", "JCLY", "JCOM", "JCP", "JCTCF", "JDO", "JDST", "JDSU", "JEC", "JEF", "JFBC", "JFROF",
			"JHG", "JHX", "JILL", "JJSF", "JKHY", "JLG", "JMBI", "JMED", "JMPLF", "JMPLY", "JNC", "JNES", "JNJ", "JNPR",
			"JNUG", "JNY", "JOB", "JOBS", "JOE", "JOSB", "JOY", "JOYG", "JP", "JPM", "JPSWY", "JPX", "JRCC", "JST",
			"JUNR", "JUVF", "JW-A", "JW-B", "JWA", "JWG", "JWN", "JXSB", "K", "KAI", "KANA", "KARE", "KATE", "KATY",
			"KB", "KBAL", "KBALB", "KBH", "KCDMY", "KCG", "KCLI", "KCP", "KCRPY", "KDDIY", "KDN", "KEFI", "KEI", "KEM",
			"KENT", "KEWL", "KEX", "KEY", "KEYUF", "KF", "KFRC", "KFS", "KFYP", "KG", "KGC", "KGFHY", "KHI", "KHOLY",
			"KID", "KIDD", "KILN", "KIM", "KIND", "KING", "KISB", "KKD", "KKPNY", "KLAC", "KLBAY", "KLIC", "KLYCY",
			"KMB", "KMBNY", "KME", "KMERF", "KMG", "KMP", "KMPR", "KMR", "KMT", "KMTUY", "KMX", "KNBWY", "KNCAY", "KND",
			"KNGT", "KNOW", "KNPRD", "KNX", "KNYJF", "KO", "KOF", "KOLD", "KOPN", "KOSS", "KPELY", "KPN", "KR", "KREM",
			"KREVF", "KRG", "KRNY", "KRO", "KRON", "KROTY", "KRU", "KS", "KSBI", "KSS", "KSU", "KSWS", "KTHN", "KTYB",
			"KUBTY", "KV-A", "KV-B", "KVA", "KWBT", "KWERF", "KWK", "KWT", "KYO", "L", "LABL", "LABU", "LACO", "LAIG",
			"LAMR", "LANC", "LAND", "LARK", "LAS", "LB", "LBAI", "LBJ", "LBRT", "LBTYA", "LBTYB", "LBTYK", "LBUY",
			"LCAV", "LCI", "LCII", "LCNB", "LCOS", "LCUT", "LDL", "LDOS", "LDP", "LEA", "LECO", "LEE", "LEG", "LEH",
			"LEHMQ", "LEN", "LEN-B", "LENS", "LESAF", "LFC", "LFCO", "LFL", "LFRGY", "LFUGY", "LFUS", "LFZA", "LG",
			"LGCHF", "LGHT", "LGND", "LGTO", "LH", "LHMS", "LHSP", "LIC", "LICT", "LIFE", "LIFZF", "LIHR", "LINK",
			"LION", "LIT", "LIV", "LIVE", "LIZ", "LKFN", "LKQ", "LKQX", "LKYSD", "LLL", "LLTC", "LLY", "LM", "LMCA",
			"LMGA", "LMNR", "LMSC", "LMT", "LNBB", "LNC", "LNCR", "LNG", "LNN", "LNT", "LO", "LOGI", "LOGIY", "LOR",
			"LORL", "LOW", "LPHI", "LPKFF", "LPMA", "LPX", "LQU", "LRCX", "LRLCY", "LRSND", "LRW", "LSBI", "LSCC",
			"LSI", "LSTR", "LTD", "LTKBF", "LTL", "LTLB", "LTR", "LTRE", "LTXB", "LU", "LUB", "LUFK", "LUK", "LUKOY",
			"LULU", "LUNMF", "LUV", "LUX", "LVCI", "LVLT", "LVMHY", "LVNTA", "LVNTB", "LVTL", "LWAY", "LXK", "LYBC",
			"LYSDY", "LYSFF", "LYTS", "LZ", "LZB", "LZRFY", "M", "MA", "MAAL", "MAAX", "MAGS", "MAHI", "MAIN", "MAKSY",
			"MANH", "MANU", "MAPS", "MAR", "MARA", "MARY", "MAS", "MASB", "MASC", "MAT", "MATR", "MATW", "MATX", "MAXS",
			"MAY", "MBCN", "MBCRQ", "MBFI", "MBI", "MBNY", "MBRG", "MBRTD", "MBT", "MBTF", "MBVA", "MBVT", "MBWM", "MC",
			"MCBC", "MCBI", "MCD", "MCFUF", "MCHP", "MCI", "MCK", "MCLD", "MCO", "MCRI", "MCRL", "MCRS", "MCS", "MCWED",
			"MCY", "MD", "MDC", "MDCA", "MDCI", "MDLL", "MDLZ", "MDNB", "MDP", "MDR", "MDSN", "MDSO", "MDT", "MDTK",
			"MDU", "MDVN", "MEAD", "MEAOD", "MEAOF", "MEAS", "MEDI", "MEDQ", "MEDX", "MEG", "MEGO", "MEI", "MEL",
			"MELA", "MENT", "MER", "MERC", "MERQ", "MERX", "MESAQ", "METR", "MFBP", "MFC", "MFCB", "MFCO", "MFE",
			"MFLA", "MFLR", "MFMLD", "MFNC", "MFNX", "MGA", "MGAFF", "MGAM", "MGEE", "MGG", "MGI", "MGIC", "MGM",
			"MGPI", "MGRC", "MHFI", "MHH", "MHK", "MHO", "MHP", "MHS", "MI", "MICC", "MIDD", "MIDU", "MIDZ", "MIELY",
			"MIICF", "MIK", "MIKE", "MIL", "MINI", "MITEY", "MITY", "MKC", "MKGAF", "MKTAY", "MKTY", "MLAN", "MLEA",
			"MLGF", "MLHR", "MLI", "MLN", "MLNK", "MLNM", "MLR", "MLSPF", "MLU", "MLYBY", "MMAC", "MMC", "MMM", "MMP",
			"MMPT", "MMRTY", "MMS", "MMSI", "MNAT", "MNBC", "MNBT", "MNC", "MNI", "MNMD", "MNRK", "MNRO", "MNST", "MNT",
			"MNTR", "MNX", "MNXBY", "MO", "MOCO", "MOD", "MOG-A", "MOGA", "MOGN", "MOH", "MOLX", "MOLXA", "MON", "MOS",
			"MOT", "MOV", "MOVI", "MPAD", "MPB", "MPC", "MPGSD", "MPL", "MPO", "MPR", "MPWR", "MPX", "MRBK", "MRCY",
			"MRGO", "MRK", "MRO", "MRTN", "MRVC", "MRVL", "MRVNY", "MRX", "MS", "MSA", "MSCC", "MSEX", "MSFG", "MSFT",
			"MSGI", "MSI", "MSL", "MSM", "MSPG", "MSS", "MT", "MTB", "MTEX", "MTG", "MTH", "MTK", "MTLG", "MTLM",
			"MTLQU", "MTON", "MTP", "MTRAF", "MTRX", "MTSC", "MTSN", "MTW", "MTWF", "MTX", "MTZ", "MU", "MUR", "MUSE",
			"MVBF", "MVL", "MVLY", "MVSN", "MVV", "MW", "MWAV", "MWD", "MWE", "MWJ", "MWN", "MWV", "MXC", "MXCYY",
			"MXIM", "MXPT", "MXT", "MXTOF", "MXWL", "MYE", "MYGN", "MYL", "MZDAY", "MZZ", "NABZY", "NADX", "NARA",
			"NASB", "NATI", "NAUT", "NAV", "NAVG", "NBAN", "NBBC", "NBCT", "NBG", "NBL", "NBR", "NBRXF", "NBTB", "NBY",
			"NC", "NCC", "NCDR", "NCEN", "NCR", "NCS", "NCT", "NCTA", "NCXS", "NDBKY", "NDEKY", "NDGT", "NDN", "NDSN",
			"NDZ", "NE", "NEE", "NEG", "NEM", "NEN", "NEOG", "NETC", "NETE", "NETL", "NEU", "NEWH", "NEWL", "NEWP",
			"NEWT", "NFB", "NFG", "NFI", "NFLX", "NFX", "NGA", "NGE", "NGG", "NHF", "NHID", "NHLC", "NHP", "NHTB",
			"NHTC", "NHY", "NHYDY", "NHYKF", "NI", "NICE", "NICK", "NIDB", "NIHD", "NIHK", "NILSY", "NIPNY", "NIS",
			"NISTF", "NITE", "NJ", "NJR", "NKE", "NKSH", "NKTR", "NLR", "NLS", "NMRX", "NMSS", "NMST", "NN", "NNBR",
			"NOBH", "NOBL", "NOC", "NOK", "NOV", "NOVAD", "NOVB", "NOVC", "NOVL", "NOVS", "NPBC", "NPPXF", "NPSN",
			"NPSNY", "NR", "NRF", "NRG", "NRGP", "NRGY", "NRIM", "NRP", "NRPH", "NRZ", "NSANY", "NSARO", "NSC", "NSEC",
			"NSFC", "NSIT", "NSM", "NSOL", "NSP", "NSRGY", "NSSC", "NST", "NT", "NTAP", "NTBK", "NTE", "NTES", "NTIOF",
			"NTLI", "NTLS", "NTP", "NTRS", "NTZ", "NUBK", "NUE", "NUGT", "NUHC", "NURO", "NVDA", "NVE", "NVEC", "NVIC",
			"NVIV", "NVLN", "NVLS", "NVO", "NVS", "NVUS", "NVZMF", "NWBI", "NWFL", "NWIN", "NWL", "NWN", "NWS", "NWS-A",
			"NWSA", "NWSB", "NWYF", "NX", "NXCR", "NXGPF", "NXLK", "NXTL", "NXY", "NYB", "NYCB", "NYF", "NYFX", "NYLD",
			"NYMT", "NYNY", "NYT", "NZ", "NZSTF", "NZT", "NZTCY", "O", "OA", "OAKT", "OART", "OATS", "OBCI", "OCAS",
			"OCC", "OCEED", "OCFC", "OCLN", "OCLND", "OCN", "OCR", "ODC", "ODFL", "ODMTY", "ODP", "ODSY", "OEH", "OFG",
			"OFISD", "OGE", "OGZPY", "OIBR", "OIH", "OII", "OIL", "OILT", "OIS", "OKE", "OKS", "OKSB", "OLBK", "OLCLY",
			"OLDB", "OLGC", "OLN", "OLTCX", "OMC", "OMG", "OMI", "OMVKY", "OMVS", "ONB", "ONFC", "ONP", "ONXS", "OOO",
			"OPHLY", "OPLK", "OPOF", "OPTN", "OPWV", "OPYGY", "ORAGD", "ORAGF", "ORBK", "ORBN", "ORCH", "ORCL", "ORCT",
			"OREX", "ORI", "ORIT", "ORKLY", "ORLY", "ORRF", "ORSDF", "OSBC", "OSBI", "OSCUF", "OSGIQ", "OSHC", "OSK",
			"OSTE", "OTCA", "OTEX", "OTRKB", "OTTR", "OVBC", "OVCHF", "OVLY", "OVTI", "OXM", "OXY", "OZRK", "PAA",
			"PACV", "PAG", "PAGP", "PAL", "PALM", "PANRA", "PAR", "PATD", "PATK", "PATR", "PAYX", "PB", "PBCO", "PBCT",
			"PBG", "PBHC", "PBI", "PBIB", "PBIO", "PBIOD", "PBNI", "PBOXD", "PBR", "PBR-A", "PBSFY", "PBYI", "PC",
			"PCAR", "PCBC", "PCBK", "PCBS", "PCCC", "PCCWY", "PCG", "PCH", "PCL", "PCLE", "PCP", "PCRFY", "PCS", "PCU",
			"PCZ", "PD", "PDA", "PDCO", "PDLI", "PDO", "PDS", "PDX", "PEB", "PEBK", "PEBO", "PED", "PEDE", "PEG",
			"PEGA", "PEGS", "PENG", "PENN", "PENX", "PEO", "PEP", "PERY", "PESI", "PETM", "PETS", "PEYUF", "PFB",
			"PFBC", "PFBI", "PFCB", "PFE", "PFGC", "PFIS", "PFLC", "PFSB", "PG", "PGC", "PGF", "PGN", "PGNT", "PGR",
			"PGS", "PGSVY", "PGTV", "PH", "PHC", "PHCC", "PHCM", "PHG", "PHGUF", "PHI", "PHJMF", "PHLY", "PHM", "PHS",
			"PHX", "PHYC", "PIAIF", "PICK", "PII", "PILLD", "PIR", "PIXR", "PKBK", "PKE", "PKI", "PKIN", "PL", "PLAB",
			"PLBC", "PLCC", "PLCM", "PLD", "PLMD", "PLT", "PLUS", "PLXP", "PLXS", "PMA", "PMBC", "PMCS", "PMD", "PMFG",
			"PMI", "PMTC", "PNBC", "PNBK", "PNC", "PNCYL", "PNFP", "PNG", "PNGAY", "PNK", "PNLYY", "PNM", "PNN", "PNNW",
			"PNR", "PNRA", "PNS", "PNTR", "PNX", "PNY", "POG", "POGLY", "POM", "POOL", "POPE", "POS", "POST", "POT",
			"POWI", "PPBN", "PPCH", "PPCHD", "PPCYY", "PPDI", "PPERY", "PPG", "PPH", "PPL", "PPLL", "PPRO", "PQEFF",
			"PR", "PRA", "PRAA", "PRCP", "PRGN", "PRGO", "PRGS", "PRGX", "PRHC", "PRK", "PRMX", "PROV", "PROX", "PRPO",
			"PRSF", "PRSP", "PRST", "PRTK", "PRVT", "PRXL", "PSBG", "PSBQ", "PSC", "PSDI", "PSGTY", "PSID", "PSIDD",
			"PSIX", "PSMMF", "PSQ", "PSS", "PSTB", "PSTI", "PSTX", "PSUN", "PSYS", "PT", "PTAIY", "PTBI", "PTBS", "PTC",
			"PTEN", "PTIX", "PTORD", "PTORF", "PTX", "PUBGY", "PULB", "PULS", "PUMA", "PUSH", "PVA", "PVFC", "PVH",
			"PVLY", "PVN", "PVR", "PVTB", "PWAV", "PWC", "PWER", "PWOD", "PWR", "PX", "PXCM", "PXFG", "PYCFF", "PZE",
			"PZOO", "PZOOD", "PZZA", "Q", "QABSY", "QADB", "QBAK", "QBCRF", "QCOM", "QCOR", "QCRH", "QD", "QEPC",
			"QGEN", "QGENF", "QID", "QLD", "QLGC", "QLTI", "QNBC", "QNTO", "QQQ", "QQQE", "QQQQ", "QRSI", "QSII", "QTM",
			"QUMU", "QWEST", "QWST", "QXLC", "R", "RAD", "RADIF", "RAH", "RAI", "RANJY", "RARE", "RAS", "RATL", "RAVN",
			"RAZF", "RBA", "RBAK", "RBC", "RBCAA", "RBCB", "RBCN", "RBGLY", "RBIN", "RBKV", "RBN", "RBPAA", "RCI",
			"RCII", "RCKB", "RCL", "RCMT", "RCRRF", "RDEIF", "RDN", "RDS-B", "RDSMY", "RDWR", "RDY", "REBC", "RECN",
			"REIS", "RELV", "RELX", "REM", "REMC", "REMX", "RENG", "RENX", "REP", "REPYF", "REPYY", "RES", "RESP",
			"RESY", "RETL", "REV", "REW", "REX", "REXI", "REXL", "REXMY", "RF", "RFIL", "RFMD", "RG", "RGA", "RGCO",
			"RGF", "RGIS", "RGR", "RGS", "RHAT", "RHAYY", "RHB", "RHBT", "RHHBY", "RHI", "RHNO", "RHP", "RI", "RICOY",
			"RIF", "RIG", "RIMG", "RIMM", "RIN", "RING", "RIO", "RIV", "RIVR", "RJF", "RKH", "RKT", "RLI", "RLRN",
			"RMCF", "RMCI", "RMD", "RMGC", "RNBO", "RNN", "RNR", "RNST", "RNT", "RNWK", "ROCK", "ROCM", "ROG", "ROK",
			"ROL", "ROM", "ROP", "ROSG", "ROSS", "ROST", "ROYL", "RPM", "RRC", "RRD", "RS", "RSAIF", "RSAM", "RSAS",
			"RSC", "RSCR", "RSG", "RSH", "RSLN", "RSO", "RSP", "RSTI", "RSXJ", "RSYS", "RT", "RTH", "RTI", "RTK", "RTN",
			"RTOKY", "RTOXF", "RTOXY", "RTP", "RUK", "RUSHA", "RUSHB", "RUSL", "RUSS", "RVBD", "RVI", "RVLT", "RVSB",
			"RVVY", "RWJ", "RWK", "RWL", "RWM", "RWR", "RX", "RXD", "RXL", "RY", "RYAAY", "RYAN", "RYCEF", "RYCEY",
			"RYL", "RYN", "S", "SAA", "SABB", "SAFC", "SAFE", "SAFM", "SAFRY", "SAH", "SAIA", "SAN", "SANM", "SAP",
			"SAPE", "SAPIF", "SAR", "SASR", "SATCQ", "SAVB", "SAWS", "SAY", "SBAZ", "SBB", "SBBX", "SBCF", "SBFC",
			"SBFG", "SBGA", "SBGI", "SBHGF", "SBIB", "SBIT", "SBKC", "SBL", "SBLK", "SBRA", "SBRBF", "SBRCY", "SBS",
			"SBSE", "SBSI", "SBUX", "SCAI", "SCBT", "SCC", "SCCO", "SCF", "SCG", "SCH", "SCHL", "SCHN", "SCHW", "SCI",
			"SCIF", "SCII", "SCIXF", "SCL", "SCM", "SCNT", "SCO", "SCON", "SCOND", "SCOR", "SCSC", "SCSS", "SCVL",
			"SDA", "SDD", "SDK", "SDLI", "SDMHF", "SDOW", "SDP", "SDS", "SDVKF", "SDVKY", "SDXAY", "SEAC", "SEBL",
			"SECX", "SED", "SEDN", "SEE", "SEIC", "SEKEF", "SELB", "SEM", "SEMID", "SENEB", "SENO", "SEPR", "SEV", "SF",
			"SFA", "SFBS", "SFBTF", "SFCC", "SFD", "SFE", "SFEF", "SFES", "SFG", "SFI", "SFIV", "SFK", "SFN", "SFNC",
			"SFNCA", "SFP", "SFTBF", "SFTBY", "SFUN", "SFX", "SG", "SGA", "SGB", "SGC", "SGDE", "SGGH", "SGLRF",
			"SGPYY", "SGR", "SGSUF", "SGTZY", "SH", "SHAW", "SHBI", "SHBK", "SHCAY", "SHECY", "SHEN", "SHFL", "SHG",
			"SHI", "SHIP", "SHLM", "SHM", "SHOO", "SHPHF", "SHW", "SI", "SIAL", "SIB", "SID", "SIE", "SIF", "SIFYD",
			"SIG", "SIGI", "SIGR", "SII", "SIJ", "SIL", "SILI", "SIVB", "SIX", "SJET", "SJF", "SJH", "SJI", "SJL",
			"SJR", "SJW", "SKBO", "SKF", "SKFRY", "SKK", "SKM", "SKT", "SKUP", "SKYW", "SLB", "SLE", "SLFC", "SLFI",
			"SLGN", "SLLDY", "SLM", "SLNB", "SLOT", "SLP", "SLR", "SLV", "SLXP", "SLYV", "SM", "SMB", "SMBC", "SMDD",
			"SMDM", "SMFG", "SMFKY", "SMG", "SMGBY", "SMGZY", "SMK", "SMLL", "SMMF", "SMN", "SMPL", "SMRT", "SMSMY",
			"SMTB", "SMTC", "SMTL", "SMTOY", "SMU", "SNA", "SNBC", "SNDK", "SNE", "SNFCA", "SNHY", "SNLAY", "SNN",
			"SNP", "SNPHF", "SNPHY", "SNPS", "SNS", "SNSTA", "SNV", "SNWL", "SO", "SOBS", "SOCLF", "SOMC", "SOMLY",
			"SON", "SONA", "SONC", "SONE", "SONG", "SONS", "SOR", "SOTR", "SOXL", "SOXS", "SP", "SPA", "SPAR", "SPCO",
			"SPDN", "SPF", "SPGI", "SPIL", "SPKE", "SPLS", "SPMYY", "SPNS", "SPP", "SPPJY", "SPPR", "SPRI", "SPUU",
			"SPW", "SPXC", "SPXL", "SPXS", "SPXSF", "SPXU", "SPYV", "SQM", "SQQQ", "SR", "SRC", "SRCE", "SRCL", "SRDX",
			"SRF", "SRNA", "SRRYD", "SRS", "SRTY", "SRV", "SRX", "SRYB", "SRZ", "SSB", "SSD", "SSETD", "SSFN", "SSG",
			"SSI", "SSKN", "SSLT", "SSLTY", "SSMXF", "SSMXY", "SSNC", "SSO", "SSP", "SSREY", "SSTI", "SSYS", "STAN",
			"STAR", "STB", "STBA", "STBI", "STC", "STD", "STE", "STEI", "STEL", "STFC", "STFR", "STGS", "STHC", "STI",
			"STJ", "STKR", "STL", "STLD", "STLJF", "STLY", "STM", "STMP", "STN", "STNR", "STR", "STRA", "STRB", "STRZA",
			"STRZB", "STS", "STSA", "STT", "STV", "STWD", "STWRY", "STZ", "STZ-B", "SU", "SUBK", "SUBR", "SUG", "SUMCF",
			"SUN", "SUNW", "SUP", "SUSQ", "SUT", "SUTNY", "SUZBY", "SVBI", "SVCBY", "SVNDY", "SVNLF", "SVRA", "SVT",
			"SVU", "SVXY", "SWFT", "SWK", "SWKH", "SWM", "SWN", "SWS", "SWWC", "SWY", "SWZ", "SXC", "SXCI", "SXCL",
			"SXL", "SXT", "SYBT", "SYI", "SYK", "SYKE", "SYMC", "SYMM", "SYNA", "SYNJD", "SYNL", "SYNT", "SYTL", "SYXI",
			"SYY", "SZGPF", "SZK", "T", "TACT", "TACYY", "TALX", "TAN", "TAP", "TARO", "TARR", "TASR", "TATT", "TBAC",
			"TBFC", "TBH", "TBI", "TBIO", "TBIOD", "TBL", "TBT", "TCB", "TCBK", "TCEHY", "TCF", "TCFC", "TCHC", "TCI",
			"TCK", "TCKRF", "TCLRY", "TCO", "TCX", "TD", "TDS", "TDSC", "TE", "TECD", "TECH", "TECK", "TECL", "TECS",
			"TECU", "TECUB", "TEF", "TEFOF", "TEK", "TELNY", "TEO", "TER", "TERN", "TESS", "TEUUF", "TEVA", "TEX",
			"TFI", "TFONY", "TFS", "TFX", "TG", "TGCDD", "TGCDF", "TGD", "TGE", "TGI", "TGISQ", "TGLO", "TGNA", "TGT",
			"TGX", "TGZ", "TH", "THAFF", "THC", "THFF", "THO", "THQI", "THRD", "THVB", "TIBB", "TIE", "TIF", "TILE",
			"TIN", "TIS", "TISI", "TJX", "TK", "TKC", "TKHVY", "TKLC", "TKOMY", "TKPPY", "TKR", "TKS", "TLAB", "TLB",
			"TLCTF", "TLD", "TLGD", "TLK", "TLL", "TLM", "TLRD", "TLS", "TLSYY", "TM", "TMAV", "TMF", "TMIC", "TMICY",
			"TMK", "TMO", "TMP", "TMPHF", "TMPW", "TMS", "TMUS", "TMV", "TMX", "TNA", "TNABY", "TNB", "TNC", "TNCC",
			"TNL", "TNLX", "TNP", "TOA", "TOL", "TOM", "TOPS", "TORW", "TOT", "TOTDF", "TOTDY", "TOUS", "TOWN", "TOX",
			"TPL", "TPRFD", "TPRFF", "TQNT", "TQQQ", "TR", "TRAC", "TRB", "TRBS", "TRCB", "TREX", "TRGT", "TRH", "TRIB",
			"TRID", "TRK", "TRMB", "TRMK", "TRN", "TROLB", "TROW", "TROX", "TROXG", "TRPS", "TRR", "TRST", "TRT",
			"TRUHY", "TRV", "TRVR", "TRYIY", "TS", "TSBK", "TSCO", "TSEM", "TSFW", "TSL", "TSM", "TSN", "TSO", "TSOH",
			"TSRI", "TSS", "TSTN", "TTC", "TTDKY", "TTEK", "TTI", "TTPA", "TTT", "TTWO", "TU", "TUFRF", "TUTR", "TV",
			"TVGIA", "TVIX", "TVIZ", "TVTY", "TVX", "TWCF", "TWI", "TWIN", "TWM", "TWMC", "TWQ", "TWRI", "TWTR", "TWX",
			"TXCC", "TXHD", "TXI", "TXN", "TXRH", "TXT", "TXU", "TXUI", "TYC", "TYD", "TYH", "TYL", "TYP", "TYTMF",
			"TZA", "UA", "UAA", "UAG", "UBAB", "UBCP", "UBFO", "UBIO", "UBMI", "UBMT", "UBNK", "UBOH", "UBP", "UBR",
			"UBS", "UBSH", "UBSI", "UBT", "UCAP", "UCBH", "UCBI", "UCC", "UCD", "UCO", "UCOMA", "UCR", "UCU", "UDIG",
			"UDOW", "UDR", "UEIC", "UFCS", "UFI", "UFS", "UGAZ", "UGE", "UGI", "UGP", "UHS", "UIL", "UIS", "UL",
			"ULTXF", "UMBF", "UMC", "UMDD", "UN", "UNB", "UNCHF", "UNF", "UNFI", "UNG", "UNH", "UNIT", "UNM", "UNP",
			"UNS", "UNTD", "UNTY", "UNYF", "UOP", "UOPX", "UOY", "UPC", "UPCOY", "UPIP", "UPL", "UPM", "UPMKY", "UPRO",
			"UPW", "URA", "URBN", "URE", "URS", "URTY", "USAB", "USAI", "USAK", "USAT", "USB", "USBI", "USD", "USEE",
			"USEG", "USG", "USIX", "USLV", "USNA", "USNZY", "USPH", "USPI", "USSHF", "UST", "USTR", "UTCI", "UTEK",
			"UTHR", "UTI", "UTIW", "UTR", "UTX", "UUGRY", "UUU", "UVN", "UVSP", "UVV", "UVXY", "UWC", "UWM", "UXI",
			"UYG", "V", "VABK", "VAL", "VALE", "VALE-P", "VALEP", "VAPH", "VAR", "VBFC", "VBIV", "VC", "VCBI", "VCBP",
			"VCI", "VCO", "VCPB", "VEDL", "VERT", "VFC", "VFGI", "VGMID", "VGR", "VHC", "VHI", "VIA", "VIAB", "VIAN",
			"VIAV", "VICR", "VIDE", "VIGN", "VIIX", "VIP", "VIPS", "VIRC", "VISI", "VISX", "VITR", "VIV", "VIVHY",
			"VIVO", "VIXM", "VIXY", "VLEEF", "VLEEY", "VLGEA", "VLKAY", "VLO", "VLT", "VLY", "VMC", "VMI", "VMSI",
			"VNBC", "VNET", "VNO", "VOD", "VODPF", "VOL", "VOLV", "VOLVY", "VOO", "VPCO", "VPFG", "VREYF", "VRIO",
			"VRSN", "VRTA", "VRTB", "VRTS", "VRTX", "VRTY", "VRUS", "VRX", "VSAT", "VSBN", "VSEA", "VSEC", "VSH", "VTI",
			"VTPI", "VTPID", "VTSS", "VVC", "VVUS", "VWO", "VXDN", "VXF", "VXUP", "VXX", "VXZ", "VYFC", "VZ", "WAB",
			"WABC", "WAC", "WAFD", "WAG", "WAMX", "WASH", "WAT", "WATR", "WAYN", "WB", "WBA", "WBCO", "WBD", "WBK",
			"WBNK", "WBS", "WBSN", "WCFB", "WCII", "WCN", "WCOM", "WDC", "WDFC", "WDFN", "WDR", "WDRW", "WEBK", "WEBT",
			"WEC", "WEFIF", "WEGZY", "WEICY", "WERN", "WEYS", "WFAFF", "WFBI", "WFC", "WFM", "WFMCD", "WFMI", "WFSL",
			"WFT", "WFTBF", "WGA", "WGL", "WGLCO", "WGNB", "WGO", "WGOV", "WGR", "WH", "WHCI", "WHI", "WHJI", "WHLM",
			"WHLR", "WHR", "WIBC", "WIN", "WIND", "WIRE", "WIT", "WL", "WLK", "WLL", "WLP", "WLSE", "WLSN", "WLT",
			"WLWHY", "WM", "WMB", "WMLP", "WMMVF", "WMMVY", "WMRSD", "WMS", "WMT", "WNC", "WON", "WOOF", "WOR", "WOS",
			"WOSCY", "WOSYD", "WPP", "WPPGY", "WPSL", "WRB", "WRI", "WRLD", "WSB", "WSBA", "WSBK", "WSFS", "WSM", "WSO",
			"WSO-B", "WST", "WSTL", "WTBA", "WTFC", "WTKWY", "WTM", "WTMK", "WTNY", "WTR", "WTRS", "WTS", "WTSL",
			"WTSLA", "WTT", "WVFC", "WWD", "WWW", "WWY", "WY", "XAGDX", "XAODX", "XAR", "XBI", "XBKS", "XCLMX", "XCRFX",
			"XDHGX", "XDNIX", "XEL", "XETA", "XFHYX", "XHAVX", "XHE", "XHIHX", "XHS", "XHSAX", "XIV", "XKHIX", "XL",
			"XLA", "XLNX", "XNGSY", "XNVA", "XOM", "XPH", "XRAY", "XRIT", "XRM", "XRT", "XRX", "XSD", "XSW", "XTEX",
			"XTN", "XTO", "XTXI", "XVLTX", "XZTRX", "Y", "YAHOY", "YAKC", "YANG", "YCL", "YCS", "YDKN", "YDNT", "YGYI",
			"YGYID", "YHOO", "YLMP", "YMLP", "YORW", "YRCW", "YUM", "YWAV", "YZC", "Z", "ZAP", "ZAZA", "ZBRA", "ZF",
			"ZGNX", "ZHEXY", "ZIGO", "ZION", "ZIV", "ZIXI", "ZLC", "ZNH", "ZNT", "ZOLL", "ZOMX", "ZQK", "ZRAN", "ZSL",
			"ZSUN", "ZTCOY", "ZTR", "ZUMZ", "ZVXI" };

	public static void downloadSplits(String ticker, String server) throws Exception {
		System.out.println("Connecting to FTP server...");

		System.out.println("site:: " + server + ticker);
		String localPath = "c:/backtest/splits/";

		if (ticker.trim().length() < 1)
			return;

		URL url = null;
		try {
			url = new URL(server + ticker.trim());
			// System.out.println("server+xmlStub:: " + url);
		} catch (MalformedURLException e) {
			// System.out.println("new url");
			e.printStackTrace();
		}

		URLConnection con = null;
		try {
			System.out.println("open conection");
			con = url.openConnection();
			if (con == null) {
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(con.getInputStream());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		System.out.println("Downloading file.");

		FileOutputStream out = null;
		try {
			FileSystemUtils.createFoldersIfReqd(localPath);
			out = new FileOutputStream(localPath + "/" + ticker);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		int i = 0;
		byte[] bytesIn = new byte[1024];
		try {
			while ((i = in.read(bytesIn)) >= 0) {// while the input stream is
													// "reading" bytesIn>0
				out.write(bytesIn, 0, i);// then write.
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("File downloaded.");

	}

	public static void downloadDividends(String ticker, String server) throws Exception {
		System.out.println("Connecting to FTP server...");

		System.out.println("site:: " + server + ticker + "/");
		String localPath = "c:/backtest/dividends/";

		if (ticker.trim().length() < 1)
			return;

		URL url = null;
		try {
			url = new URL(server + ticker.trim() + "/");
			// System.out.println("server+xmlStub:: " + url);
		} catch (MalformedURLException e) {
			// System.out.println("new url");
			e.printStackTrace();
		}

		URLConnection con = null;
		try {
			System.out.println("open conection");
			con = url.openConnection();
			if (con == null) {
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(con.getInputStream());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		System.out.println("Downloading file.");

		FileOutputStream out = null;
		try {
			FileSystemUtils.createFoldersIfReqd(localPath);
			out = new FileOutputStream(localPath + "/" + ticker);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}

		int i = 0;
		byte[] bytesIn = new byte[1024];
		try {
			while ((i = in.read(bytesIn)) >= 0) {// while the input stream is
													// "reading" bytesIn>0
				out.write(bytesIn, 0, i);// then write.
			}
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {

			e.printStackTrace();
		}
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("File downloaded.");

	}

	public static void parseSplit(String ticker) throws IOException, SQLException {
		// create a second that reads an entire folder and to re-parse it. For
		// now parse based on last ticker downloaded. Than move the file to a
		// bac folder.

		NLP nlp = new NLP();
		File f = new File("c:/backtest/splits/" + ticker.replaceAll("[\r\n ]'", ""));
		String splitText = Utils.readTextFromFile(f.getAbsolutePath());
		splitText = nlp.stripHtmlTags(splitText);
		// System.out.println(splitText);
		Pattern patternDate = Pattern.compile("[01]{1}[\\d]{1}/[0123]{1}[\\d]{1}/[0912]{1}[09]{1}\t[\\d]{1,5}");

		List<String[]> listDates = nlp.getAllEndIdxAndMatchedGroupLocs(splitText, patternDate);

		String spData = "";
		File file = new File("c:/backtest/splits/s_" + ticker.replaceAll("[\r\n ]'", "") + ".csv");
		if (file.exists())
			file.delete();
		PrintWriter pw = new PrintWriter(file);

		String query = "";

		for (int i = 0; i < listDates.size(); i++) {
			spData = listDates.get(i)[1].replaceAll(" for ", "\t");
			String[] spl = spData.split("\t");
			System.out.println(spl[0] + " " + ticker.replaceAll("[\r\n ]'", "") + " " + spl[1] + " " + spl[2]);
			pw.append(spl[0] + "," + ticker.replaceAll("[\r\n ]'", "") + "," + spl[1] + "," + spl[2] + "\r");
		}
		pw.close();
		if (listDates.size() == 0)
			file.delete();
		else {

			query = "LOAD DATA INFILE 'c:/backtest/splits/s_" + ticker.replaceAll("[\r\n ]'", "") + ".csv' "
					+ "\rIgnore INTO TABLE tmp1_stocksplit " + "\rFIELDS TERMINATED BY ','"
					+ "\rLINES TERMINATED BY '\r'" + "(@var1,its,num,den)"
					+ "SET date = str_to_date(@var1, '%m/%d/%Y');";
			MysqlConnUtils.executeQuery(query);

		}
		f.delete();
	}

	public static void reParseSplits() throws IOException, SQLException {

		File folder = new File("c:/backtest/splits/");
		File[] listOfFiles = folder.listFiles();
		NLP nlp = new NLP();
		String qry = "";

		String backupDate = "20170804";
		MysqlConnUtils.executeQuery("insert ignore into stocksplit\r" + "\rselect * from tmp1_stocksplit;"
				+ "\rdrop table if exists bac_stocksplit" + backupDate + ";\r" + "create table bac_stocksplit"
				+ backupDate + " engine=myisam\r" + "select * from stocksplit;\r");

		String query = "drop table if exists tmp1_stocksplit;\r" + "create table tmp1_stocksplit engine=myisam\r"
				+ "select * from stocksplit limit 1;\r" + "truncate tmp1_stocksplit;\r";

		MysqlConnUtils.executeQuery(query);

		for (File file : listOfFiles) {
			if (file.isFile()) {
				// System.out.println(nlp.getAllMatchedGroups(file.getName(),
				// Pattern.compile("[A-Z-]{1,10}")).get(0));
				List<String> list = nlp.getAllMatchedGroups(file.getName(), Pattern.compile("[A-Z-]{1,10}"));
				if (list.size() > 0) {

					qry = "LOAD DATA INFILE 'c:/backtest/splits/s_" + list.get(0) + ".csv' "
							+ "\rIgnore INTO TABLE tmp1_stocksplit " + "\rFIELDS TERMINATED BY ','"
							+ "\rLINES TERMINATED BY '\r'" + "(@var1,its,num,den)"
							+ "SET date = str_to_date(@var1, '%m/%d/%Y');";
					MysqlConnUtils.executeQuery(qry);
				}

			}
		}

	}

	public static void downloadStocksplits() throws Exception {

		// TODO: select * from stocksplit
		// where its rlike ' |\\.' group by its;
		// #excluded above for now. Next run just these types as both cig.c and
		// cigc or bf.a or bfa

		// can also grab from file or use public tickers string array.

		String str = "call splitDownlder_procedure();";

		File file = new File("c:/backtest/symbols/tickers.txt");
		if (file.exists())
			file.delete();

		MysqlConnUtils.executeQuery(str);

		String[] tickers = Utils.readTextFromFile("c:/backtest/symbols/tickers.txt").split("\r");
		System.out.println("# of tickers=" + tickers.length);

		String backupDate = Calendar.getInstance().get(Calendar.YEAR) + "" + Calendar.getInstance().get(Calendar.MONTH)
				+ "";
		MysqlConnUtils.executeQuery("\rdrop table if exists bac_stocksplit" + backupDate + ";\r"
				+ "create table bac_stocksplit" + backupDate + " engine=myisam\r" + "select * from stocksplit;\r");

		String query = "drop table if exists tmp1_stocksplit;\r" + "CREATE TABLE `tmp1_stocksplit` (\r"
				+ "`date` date NOT NULL,\r" + "`ITS` varchar(25) NOT NULL,\r" + "`Num` double NOT NULL DEFAULT '-1',\r"
				+ "`Den` double NOT NULL DEFAULT '-1'\r" + ") ENGINE=InnoDB DEFAULT CHARSET=latin1;\r";

		MysqlConnUtils.executeQuery(query);

		String ticker = "";
		System.out.println("# of tickers=" + tickers.length);
		for (int i = 0; i < tickers.length; i++) {
			ticker = tickers[i].replaceAll("[ \r\n]", "");
			if (ticker.length() < 1)
				continue;
			downloadSplits(ticker, splitServer);
			parseSplit(ticker);
		}
		query = "insert ignore into stocksplit" + "\rselect * from tmp1_stocksplit;";

		MysqlConnUtils.executeQuery(query);

		reParseSplits();
		query = "insert ignore into stocksplit" + "\rselect * from tmp1_stocksplit;";

		MysqlConnUtils.executeQuery(query);

		// TODO: get just most recent stocksplits:
		// https://www.marketbeat.com/stock-splits/
		// TODO: get all stock splits globally - has past 3 weeks.
		// https://www.investing.com/stock-split-calendar/

	}

	public static void downloadStockDividends() throws Exception {

		// TODO: select * from stockdividend
		// where its rlike ' |\\.' group by its;
		// #excluded above for now. Next run just these types as both cig.c and
		// cigc or bf.a or bfa
		// can also grab from file or use public tickers string array.

		String str = "call dividendDownlder_PROCEDURE();";

		File file = new File("c:/backtest/symbols/div_tickers.txt");
		if (file.exists())
			file.delete();

		MysqlConnUtils.executeQuery(str);

		String[] tickers = Utils.readTextFromFile("c:/backtest/symbols/div_tickers.txt").split("\r");
		System.out.println("# of tickers=" + tickers.length);

		String backupDate = Calendar.getInstance().get(Calendar.YEAR) + "" + Calendar.getInstance().get(Calendar.MONTH)
				+ "";
		MysqlConnUtils.executeQuery(
				"\rdrop table if exists bac_stockdividend" + backupDate + ";\r" + "create table bac_stockdividend"
						+ backupDate + " engine=myisam\r" + "select * from stockdividend;\r");

		String query = "drop table if exists tmp1_stockdividend;\r" + "CREATE TABLE `tmp1_stockdividend` (\r"
				+ "`date` date NOT NULL,\r" + "`ITS` varchar(25) NOT NULL,\r" + "`Num` double NOT NULL DEFAULT '-1',\r"
				+ "`Den` double NOT NULL DEFAULT '-1'\r" + ") ENGINE=myisam DEFAULT CHARSET=latin1;\r";

		MysqlConnUtils.executeQuery(query);

		String ticker = "";
		System.out.println("# of tickers=" + tickers.length);
		for (int i = 0; i < tickers.length; i++) {
			ticker = tickers[i].replaceAll("[ \r\n]", "");
			if (ticker.length() < 1)
				continue;
			downloadDividends(ticker, dividendServer);
			// parseDividends(ticker);xxxx
		}
		query = "insert ignore into stockdividend" + "\rselect * from tmp1_stockdividend;";

		MysqlConnUtils.executeQuery(query);

		reParseDividends();
		query = "insert ignore into stockdividend" + "\rselect * from tmp1_stockdividend;";

		MysqlConnUtils.executeQuery(query);

		// TODO: get just most recent stockdividends:
		// https://www.marketbeat.com/stock-splits/
		// TODO: get all stock splits globally - has past 3 weeks.
		// https://www.investing.com/stock-split-calendar/

	}

	public static void parseDividends(String ticker) throws IOException, SQLException {
		// create a second that reads an entire folder and to re-parse it. For
		// now parse based on last ticker downloaded. Than move the file to a
		// bac folder.

		NLP nlp = new NLP();
		File f = new File("c:/backtest/splits/" + ticker.replaceAll("[\r\n ]'", ""));
		String splitText = Utils.readTextFromFile(f.getAbsolutePath());
		splitText = nlp.stripHtmlTags(splitText);
		// System.out.println(splitText);
		Pattern patternDate = Pattern.compile("[01]{1}[\\d]{1}/[0123]{1}[\\d]{1}/[0912]{1}[09]{1}\t[\\d]{1,5}");

		List<String[]> listDates = nlp.getAllEndIdxAndMatchedGroupLocs(splitText, patternDate);

		String spData = "";
		File file = new File("c:/backtest/splits/s_" + ticker.replaceAll("[\r\n ]'", "") + ".csv");
		if (file.exists())
			file.delete();
		PrintWriter pw = new PrintWriter(file);

		String query = "";

		for (int i = 0; i < listDates.size(); i++) {
			spData = listDates.get(i)[1].replaceAll(" for ", "\t");
			String[] spl = spData.split("\t");
			System.out.println(spl[0] + " " + ticker.replaceAll("[\r\n ]'", "") + " " + spl[1] + " " + spl[2]);
			pw.append(spl[0] + "," + ticker.replaceAll("[\r\n ]'", "") + "," + spl[1] + "," + spl[2] + "\r");
		}
		pw.close();
		if (listDates.size() == 0)
			file.delete();
		else {

			query = "LOAD DATA INFILE 'c:/backtest/splits/s_" + ticker.replaceAll("[\r\n ]'", "") + ".csv' "
					+ "\rIgnore INTO TABLE tmp1_stocksplit " + "\rFIELDS TERMINATED BY ','"
					+ "\rLINES TERMINATED BY '\r'" + "(@var1,its,num,den)"
					+ "SET date = str_to_date(@var1, '%m/%d/%Y');";
			MysqlConnUtils.executeQuery(query);

		}
		f.delete();
	}

	public static void reParseDividends() throws IOException, SQLException {

		File folder = new File("c:/backtest/splits/");
		File[] listOfFiles = folder.listFiles();
		NLP nlp = new NLP();
		String qry = "";

		String backupDate = "20170804";
		MysqlConnUtils.executeQuery("insert ignore into stocksplit\r" + "\rselect * from tmp1_stocksplit;"
				+ "\rdrop table if exists bac_stocksplit" + backupDate + ";\r" + "create table bac_stocksplit"
				+ backupDate + " engine=myisam\r" + "select * from stocksplit;\r");

		String query = "drop table if exists tmp1_stocksplit;\r" + "create table tmp1_stocksplit engine=myisam\r"
				+ "select * from stocksplit limit 1;\r" + "truncate tmp1_stocksplit;\r";

		MysqlConnUtils.executeQuery(query);

		for (File file : listOfFiles) {
			if (file.isFile()) {
				// System.out.println(nlp.getAllMatchedGroups(file.getName(),
				// Pattern.compile("[A-Z-]{1,10}")).get(0));
				List<String> list = nlp.getAllMatchedGroups(file.getName(), Pattern.compile("[A-Z-]{1,10}"));
				if (list.size() > 0) {

					qry = "LOAD DATA INFILE 'c:/backtest/splits/s_" + list.get(0) + ".csv' "
							+ "\rIgnore INTO TABLE tmp1_stocksplit " + "\rFIELDS TERMINATED BY ','"
							+ "\rLINES TERMINATED BY '\r'" + "(@var1,its,num,den)"
							+ "SET date = str_to_date(@var1, '%m/%d/%Y');";
					MysqlConnUtils.executeQuery(qry);
				}

			}
		}

	}

	public static void main(String[] args) throws Exception {

		// downloadStocksplits();
		// SEE XXX. DIVIDENDCHANNELS ONLY LETS YOU GET A COUPLE BEFORE IT BLOCKS ME.
		// downloadStockDividends();

	}
}
